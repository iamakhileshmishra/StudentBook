package com.schoolerp.modules.fee.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.schoolerp.modules.fee.dto.CollectFeeRequest;
import com.schoolerp.modules.fee.dto.FeeDuesResponse;
import com.schoolerp.modules.fee.entity.MonthlyFeeStructure;
import com.schoolerp.modules.fee.entity.PaymentLineItem;
import com.schoolerp.modules.fee.entity.PaymentTransaction;
import com.schoolerp.modules.fee.repository.PaymentLineItemRepository;
import com.schoolerp.modules.fee.repository.PaymentTransactionRepository;
import com.schoolerp.modules.settings.dto.LateFeeRuleDto;
import com.schoolerp.modules.student.entity.Student;
import com.schoolerp.modules.student.entity.StudentEnrollment;
import com.schoolerp.modules.student.repository.StudentEnrollmentRepository;
import com.schoolerp.modules.student.repository.StudentRepository;
import com.schoolerp.shared.exception.BusinessException;
import com.schoolerp.shared.exception.ResourceNotFoundException;
import com.schoolerp.shared.security.TenantContext;
import com.schoolerp.shared.time.SchoolClock;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class FeeCollectionService {

    private static final String ARREARS_CATEGORY = "Previous Year Arrears";

    private final StudentRepository studentRepository;
    private final StudentEnrollmentRepository studentEnrollmentRepository;
    private final PaymentTransactionRepository paymentTransactionRepository;
    private final PaymentLineItemRepository paymentLineItemRepository;
    private final FeeStructureLookup feeStructureLookup;
    private final JdbcTemplate jdbcTemplate;
    private final ObjectMapper objectMapper;
    private final SchoolClock clock;

    public FeeDuesResponse getDues(UUID studentId, int month, int year) {
        UUID schoolId = TenantContext.getSchoolId();
        validatePeriod(month, year);

        Student student = findStudent(studentId, schoolId);

        var existingPayment = paymentTransactionRepository
                .findFirstByStudentIdAndCollectionMonthAndCollectionYearAndStatusOrderByConfirmedAtDesc(
                        studentId, month, year, "CONFIRMED");
        if (existingPayment.isPresent()) {
            return FeeDuesResponse.builder()
                    .alreadyPaid(true)
                    .paidTransactionId(existingPayment.get().getId())
                    .paidReceiptNumber(existingPayment.get().getReceiptNumber())
                    .build();
        }

        requireMonthInCurrentYear(schoolId, month, year);
        StudentEnrollment enrollment = findCurrentEnrollment(studentId, schoolId);
        List<MonthlyFeeStructure> rows =
                feeStructureLookup.activeRows(schoolId, enrollment.getAcademicYearId(), enrollment.getClassId());
        Map<UUID, String> categoryNames = feeStructureLookup.activeCategoryNames(schoolId);

        List<FeeDuesResponse.LineItem> lineItems = new ArrayList<>();
        BigDecimal subtotal = BigDecimal.ZERO;
        for (MonthlyFeeStructure row : rows) {
            lineItems.add(FeeDuesResponse.LineItem.builder()
                    .feeCategoryId(row.getFeeCategoryId())
                    .feeCategoryName(categoryNames.getOrDefault(row.getFeeCategoryId(), "Fee"))
                    .amount(row.getDefaultAmount())
                    .build());
            subtotal = subtotal.add(row.getDefaultAmount());
        }

        BigDecimal previousDues = nz(enrollment.getPreviousDues());
        if (previousDues.signum() > 0) {
            subtotal = subtotal.add(previousDues);
            lineItems.add(FeeDuesResponse.LineItem.builder()
                    .feeCategoryId(getOrCreateArrearsCategoryId(schoolId))
                    .feeCategoryName(ARREARS_CATEGORY)
                    .amount(previousDues)
                    .build());
        }

        return FeeDuesResponse.builder()
                .subtotal(subtotal)
                .expectedLateFee(calculateLateFee(schoolId, month, year))
                .expectedConcession(nz(student.getConcessionAmount()))
                .lineItems(lineItems)
                .build();
    }

    @Transactional
    public PaymentTransaction collectFee(CollectFeeRequest request, UUID collectedByUserId) {
        UUID schoolId = TenantContext.getSchoolId();
        validatePeriod(request.getMonth(), request.getYear());

        Student student = findStudent(request.getStudentId(), schoolId);
        requireMonthInCurrentYear(schoolId, request.getMonth(), request.getYear());
        StudentEnrollment enrollment = findCurrentEnrollment(student.getId(), schoolId);

        // BUG-10: no new fees for students who have left or graduated
        if (!"ACTIVE".equals(student.getStatus()) || !"ACTIVE".equals(enrollment.getStatus())) {
            throw new BusinessException("Fees can't be collected for " + student.getFirstName()
                    + " because the student is no longer active (" + student.getStatus() + ").");
        }

        if (paymentTransactionRepository.existsByStudentIdAndCollectionMonthAndCollectionYearAndStatus(
                student.getId(), request.getMonth(), request.getYear(), "CONFIRMED")) {
            throw new BusinessException("The fee for this month has already been collected.");
        }

        List<MonthlyFeeStructure> rows =
                feeStructureLookup.activeRows(schoolId, enrollment.getAcademicYearId(), enrollment.getClassId());
        BigDecimal subtotal = rows.stream()
                .map(MonthlyFeeStructure::getDefaultAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal previousDues = nz(enrollment.getPreviousDues());
        if (previousDues.signum() > 0) {
            subtotal = subtotal.add(previousDues);
        }

        // BUG-03: amounts come from the cashier, so bound them server-side
        BigDecimal concession = nz(request.getConcessionAmount());
        BigDecimal lateFee = nz(request.getLateFee());
        if (concession.compareTo(subtotal) > 0) {
            throw new BusinessException("Concession (₹" + concession.toPlainString()
                    + ") cannot be more than the fee (₹" + subtotal.toPlainString() + ").");
        }
        BigDecimal grandTotal = subtotal.subtract(concession).add(lateFee);

        LocalDate today = clock.today();
        PaymentTransaction transaction = PaymentTransaction.builder()
                .schoolId(schoolId)
                .receiptNumber(buildReceiptNumber(schoolId, student, today))
                .studentId(student.getId())
                .enrollmentId(enrollment.getId())
                .academicYearId(enrollment.getAcademicYearId())
                .collectionMonth(request.getMonth())
                .collectionYear(request.getYear())
                .previousBalance(BigDecimal.ZERO)
                .subtotal(subtotal)
                .concessionApplied(concession)
                .lateFee(lateFee)
                .grandTotal(grandTotal)
                .amountPaid(grandTotal)
                .remainingBalance(BigDecimal.ZERO)
                .paymentMethod(request.getPaymentMethod())
                .paymentReference(request.getPaymentReference())
                .paymentDate(today)
                .remarks(request.getRemarks())
                .status("CONFIRMED")
                .collectedBy(collectedByUserId)
                .confirmedAt(Instant.now())
                .build();

        PaymentTransaction saved;
        try {
            // Flush now so a concurrent duplicate hits the unique index here (BUG-04)
            saved = paymentTransactionRepository.saveAndFlush(transaction);
        } catch (DataIntegrityViolationException e) {
            throw new BusinessException("The fee for this month has already been collected.");
        }

        List<PaymentLineItem> lineItems = new ArrayList<>(rows.stream().map(row ->
                PaymentLineItem.builder()
                        .schoolId(schoolId)
                        .transactionId(saved.getId())
                        .feeCategoryId(row.getFeeCategoryId())
                        .amountCharged(row.getDefaultAmount())
                        .build()
        ).toList());

        if (previousDues.signum() > 0) {
            lineItems.add(PaymentLineItem.builder()
                    .schoolId(schoolId)
                    .transactionId(saved.getId())
                    .feeCategoryId(getOrCreateArrearsCategoryId(schoolId))
                    .amountCharged(previousDues)
                    .build());
            enrollment.setPreviousDues(BigDecimal.ZERO);
            studentEnrollmentRepository.save(enrollment);
        }
        paymentLineItemRepository.saveAll(lineItems);

        return saved;
    }

    // ── Helpers ──────────────────────────────────────────────────────────────

    private static void validatePeriod(int month, int year) {
        if (month < 1 || month > 12) {
            throw new BusinessException("Month must be between 1 and 12.");
        }
        if (year < 2000 || year > 2100) {
            throw new BusinessException("Enter a valid year.");
        }
    }

    private Student findStudent(UUID studentId, UUID schoolId) {
        return studentRepository.findById(studentId)
                .filter(s -> s.getSchoolId().equals(schoolId))
                .orElseThrow(() -> new ResourceNotFoundException("Student not found"));
    }

    /**
     * BUG-19: the month must fall inside the current academic year. Earlier months
     * belong to a closed year (their dues were carried forward as arrears at
     * promotion); charging them against the new enrollment double-bills.
     */
    private void requireMonthInCurrentYear(UUID schoolId, int month, int year) {
        var row = jdbcTemplate.queryForList(
                "SELECT start_date, end_date, name FROM academic_years WHERE school_id = ? AND is_current = true", schoolId);
        if (row.isEmpty()) {
            throw new BusinessException("No current academic year is configured.");
        }
        YearMonth start = YearMonth.from(((java.sql.Date) row.get(0).get("start_date")).toLocalDate());
        YearMonth end = YearMonth.from(((java.sql.Date) row.get(0).get("end_date")).toLocalDate());
        YearMonth period = YearMonth.of(year, month);
        if (period.isBefore(start) || period.isAfter(end)) {
            DateTimeFormatter f = DateTimeFormatter.ofPattern("MMM yyyy");
            throw new BusinessException(period.format(f) + " is outside the current academic year "
                    + row.get(0).get("name") + " (" + start.format(f) + " – " + end.format(f) + ").");
        }
    }

    private StudentEnrollment findCurrentEnrollment(UUID studentId, UUID schoolId) {
        UUID academicYearId = getCurrentAcademicYearId(schoolId);
        return studentEnrollmentRepository
                .findByStudentIdAndAcademicYearIdAndSchoolId(studentId, academicYearId, schoolId)
                .orElseThrow(() -> new BusinessException("This student is not enrolled in the current academic year."));
    }

    /** Late fee from the school's rules, based on today's date in the school's time zone. */
    private BigDecimal calculateLateFee(UUID schoolId, int month, int year) {
        Integer dueDay = null;
        String rulesJson = null;
        var schoolRow = jdbcTemplate.queryForList(
                "SELECT fee_due_date, late_fee_rules::text AS rules FROM schools WHERE id = ?", schoolId);
        if (!schoolRow.isEmpty()) {
            dueDay = (Integer) schoolRow.get(0).get("fee_due_date");
            rulesJson = (String) schoolRow.get(0).get("rules");
        }
        if (dueDay == null) dueDay = 10;

        int targetDay = Math.min(dueDay, YearMonth.of(year, month).lengthOfMonth());
        LocalDate dueDate = LocalDate.of(year, month, targetDay);
        LocalDate today = clock.today();
        if (!today.isAfter(dueDate) || rulesJson == null || rulesJson.isBlank() || rulesJson.equals("[]")) {
            return BigDecimal.ZERO;
        }

        long daysLate = ChronoUnit.DAYS.between(dueDate, today);
        List<LateFeeRuleDto> rules;
        try {
            rules = objectMapper.readValue(rulesJson, new TypeReference<List<LateFeeRuleDto>>() {});
        } catch (Exception e) {
            log.warn("School {} has unreadable late fee rules; no late fee applied", schoolId, e);
            return BigDecimal.ZERO;
        }

        BigDecimal applicableFee = BigDecimal.ZERO;
        LateFeeRuleDto highestRule = null;
        for (LateFeeRuleDto rule : rules) {
            if (highestRule == null || rule.getEndDaysLate() > highestRule.getEndDaysLate()) {
                highestRule = rule;
            }
            if (daysLate >= rule.getStartDaysLate() && daysLate <= rule.getEndDaysLate()) {
                applicableFee = rule.getAmount();
            }
        }
        // Beyond the highest tier, the highest tier applies
        if (applicableFee.signum() == 0 && highestRule != null && daysLate > highestRule.getEndDaysLate()) {
            applicableFee = highestRule.getAmount();
        }
        return nz(applicableFee);
    }

    /** SHORT-YYYY-MMDD-ADMNO-XXXX; the column is wide enough since V14 (BUG-05). */
    private String buildReceiptNumber(UUID schoolId, Student student, LocalDate today) {
        String shortName = jdbcTemplate.query(
                "SELECT short_name FROM schools WHERE id = ?",
                rs -> rs.next() ? rs.getString(1) : null, schoolId);
        if (shortName == null || shortName.isBlank()) shortName = "SCH";
        shortName = shortName.replaceAll("[^A-Za-z0-9]", "").toUpperCase();
        if (shortName.length() > 10) shortName = shortName.substring(0, 10);

        return String.format("%s-%d-%s-%s-%s",
                shortName,
                today.getYear(),
                today.format(DateTimeFormatter.ofPattern("MMdd")),
                student.getAdmissionNumber(),
                UUID.randomUUID().toString().substring(0, 4).toUpperCase());
    }

    private UUID getCurrentAcademicYearId(UUID schoolId) {
        List<UUID> activeYears = jdbcTemplate.query(
                "SELECT id FROM academic_years WHERE school_id = ? AND is_current = true LIMIT 1",
                (rs, rowNum) -> rs.getObject("id", UUID.class),
                schoolId);
        if (activeYears.isEmpty()) {
            throw new BusinessException("No current academic year is configured.");
        }
        return activeYears.get(0);
    }

    /** Category used for carried-forward arrears; created on first use. */
    private UUID getOrCreateArrearsCategoryId(UUID schoolId) {
        List<UUID> ids = jdbcTemplate.query(
                "SELECT id FROM fee_categories WHERE school_id = ? AND name = ?",
                (rs, rowNum) -> rs.getObject("id", UUID.class),
                schoolId, ARREARS_CATEGORY);
        if (!ids.isEmpty()) {
            return ids.get(0);
        }
        // BUG-15: no fake fallback id — a failure here should surface, not break a later FK
        UUID newId = UUID.randomUUID();
        jdbcTemplate.update(
                "INSERT INTO fee_categories (id, school_id, name, display_order, is_active, created_at) VALUES (?, ?, ?, 999, true, NOW())",
                newId, schoolId, ARREARS_CATEGORY);
        return newId;
    }

    private static BigDecimal nz(BigDecimal v) {
        return v == null ? BigDecimal.ZERO : v;
    }
}
