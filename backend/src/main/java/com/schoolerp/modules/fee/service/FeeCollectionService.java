package com.schoolerp.modules.fee.service;

import com.schoolerp.modules.fee.dto.CollectFeeRequest;
import com.schoolerp.modules.fee.dto.FeeDuesResponse;
import com.schoolerp.modules.fee.entity.PaymentLineItem;
import com.schoolerp.modules.fee.entity.PaymentTransaction;
import com.schoolerp.modules.fee.entity.MonthlyFeeStructure;
import com.schoolerp.modules.fee.repository.PaymentLineItemRepository;
import com.schoolerp.modules.fee.repository.PaymentTransactionRepository;
import com.schoolerp.modules.fee.repository.FeeCategoryRepository;
import com.schoolerp.modules.fee.repository.MonthlyFeeStructureRepository;
import com.schoolerp.modules.student.entity.Student;
import com.schoolerp.modules.student.entity.StudentEnrollment;
import com.schoolerp.modules.student.repository.StudentEnrollmentRepository;
import com.schoolerp.modules.student.repository.StudentRepository;
import com.schoolerp.shared.security.TenantContext;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class FeeCollectionService {

    private final StudentRepository studentRepository;
    private final StudentEnrollmentRepository studentEnrollmentRepository;
    private final MonthlyFeeStructureRepository monthlyFeeStructureRepository;
    private final FeeCategoryRepository feeCategoryRepository;
    private final PaymentTransactionRepository paymentTransactionRepository;
    private final PaymentLineItemRepository paymentLineItemRepository;
    private final JdbcTemplate jdbcTemplate;

    public FeeDuesResponse getDues(UUID studentId, int month, int year) {
        UUID schoolId = TenantContext.getSchoolId();

        Student student = studentRepository.findById(studentId)
                .filter(s -> s.getSchoolId().equals(schoolId))
                .orElseThrow(() -> new IllegalArgumentException("Student not found"));

        var existingPayment = paymentTransactionRepository.findFirstByStudentIdAndCollectionMonthAndCollectionYearAndStatusOrderByConfirmedAtDesc(
                studentId, month, year, "CONFIRMED");
        if (existingPayment.isPresent()) {
            return FeeDuesResponse.builder()
                    .alreadyPaid(true)
                    .paidTransactionId(existingPayment.get().getId())
                    .paidReceiptNumber(existingPayment.get().getReceiptNumber())
                    .build();
        }

        UUID academicYearId = getCurrentAcademicYearId(schoolId);
        StudentEnrollment enrollment = studentEnrollmentRepository
                .findByStudentIdAndAcademicYearIdAndSchoolId(studentId, academicYearId, schoolId)
                .orElseThrow(() -> new IllegalArgumentException("Active enrollment not found"));

        List<MonthlyFeeStructure> structures = monthlyFeeStructureRepository
                .findBySchoolIdAndAcademicYearIdAndClassIdOrderByFeeCategoryId(schoolId, enrollment.getAcademicYearId(), enrollment.getClassId());

        var categories = feeCategoryRepository.findBySchoolIdAndActiveTrueOrderByDisplayOrderAsc(schoolId)
                .stream().collect(Collectors.toMap(c -> c.getId(), c -> c.getName()));

        BigDecimal subtotal = BigDecimal.ZERO;
        List<FeeDuesResponse.LineItem> lineItems = structures.stream().map(structure -> {
            return FeeDuesResponse.LineItem.builder()
                    .feeCategoryId(structure.getFeeCategoryId())
                    .feeCategoryName(categories.getOrDefault(structure.getFeeCategoryId(), "Fee"))
                    .amount(structure.getDefaultAmount())
                    .build();
        }).collect(Collectors.toList());

        for (FeeDuesResponse.LineItem item : lineItems) {
            subtotal = subtotal.add(item.getAmount());
        }

        if (enrollment.getPreviousDues() != null && enrollment.getPreviousDues().compareTo(BigDecimal.ZERO) > 0) {
            subtotal = subtotal.add(enrollment.getPreviousDues());
            lineItems.add(FeeDuesResponse.LineItem.builder()
                    .feeCategoryId(getOrCreateArrearsCategoryId(schoolId))
                    .feeCategoryName("Previous Year Arrears")
                    .amount(enrollment.getPreviousDues())
                    .build());
        }

        BigDecimal expectedLateFee = BigDecimal.ZERO;
        
        Integer dueDay = null;
        String rulesJson = null;
        try {
            var schoolRow = jdbcTemplate.queryForMap(
                "SELECT fee_due_date, late_fee_rules FROM schools WHERE id = ?", schoolId);
            dueDay = (Integer) schoolRow.get("fee_due_date");
            rulesJson = (String) schoolRow.get("late_fee_rules");
        } catch (Exception e) {
            // ignore
        }
        
        if (dueDay == null) dueDay = 10;
        
        int targetDay = Math.min(dueDay, java.time.YearMonth.of(year, month).lengthOfMonth());
        LocalDate dueDate = LocalDate.of(year, month, targetDay);
        LocalDate today = LocalDate.now();
        
        if (today.isAfter(dueDate)) {
            long daysLate = java.time.temporal.ChronoUnit.DAYS.between(dueDate, today);
            if (rulesJson != null && !rulesJson.isEmpty() && !rulesJson.equals("[]")) {
                try {
                    com.fasterxml.jackson.databind.ObjectMapper mapper = new com.fasterxml.jackson.databind.ObjectMapper();
                    List<com.schoolerp.modules.settings.dto.LateFeeRuleDto> rules = mapper.readValue(
                        rulesJson, new com.fasterxml.jackson.core.type.TypeReference<List<com.schoolerp.modules.settings.dto.LateFeeRuleDto>>() {}
                    );
                    
                    BigDecimal applicableFee = BigDecimal.ZERO;
                    com.schoolerp.modules.settings.dto.LateFeeRuleDto highestRule = null;
                    
                    for (com.schoolerp.modules.settings.dto.LateFeeRuleDto rule : rules) {
                        if (highestRule == null || rule.getEndDaysLate() > highestRule.getEndDaysLate()) {
                            highestRule = rule;
                        }
                        if (daysLate >= rule.getStartDaysLate() && daysLate <= rule.getEndDaysLate()) {
                            applicableFee = rule.getAmount();
                        }
                    }
                    
                    // If days late exceeds the highest tier, apply the highest tier
                    if (applicableFee.compareTo(BigDecimal.ZERO) == 0 && highestRule != null && daysLate > highestRule.getEndDaysLate()) {
                        applicableFee = highestRule.getAmount();
                    }
                    expectedLateFee = applicableFee;
                } catch (Exception e) {
                    // ignore
                }
            }
        }

        BigDecimal expectedConcession = student.getConcessionAmount() != null ? student.getConcessionAmount() : BigDecimal.ZERO;

        return FeeDuesResponse.builder()
                .subtotal(subtotal)
                .expectedLateFee(expectedLateFee)
                .expectedConcession(expectedConcession)
                .lineItems(lineItems)
                .build();
    }

    @Transactional
    public PaymentTransaction collectFee(CollectFeeRequest request, UUID collectedByUserId) {
        UUID schoolId = TenantContext.getSchoolId();

        Student student = studentRepository.findById(request.getStudentId())
                .filter(s -> s.getSchoolId().equals(schoolId))
                .orElseThrow(() -> new IllegalArgumentException("Student not found"));

        if (paymentTransactionRepository.existsByStudentIdAndCollectionMonthAndCollectionYearAndStatus(
                request.getStudentId(), request.getMonth(), request.getYear(), "CONFIRMED")) {
            throw new IllegalStateException("Payment already collected for this month and year");
        }

        UUID academicYearId = getCurrentAcademicYearId(schoolId);
        StudentEnrollment enrollment = studentEnrollmentRepository
                .findByStudentIdAndAcademicYearIdAndSchoolId(request.getStudentId(), academicYearId, schoolId)
                .orElseThrow(() -> new IllegalArgumentException("Active enrollment not found"));

        String schoolShortName;
        try {
            schoolShortName = jdbcTemplate.queryForObject(
                    "SELECT short_name FROM schools WHERE id = ?", String.class, schoolId);
        } catch (Exception e) {
            schoolShortName = "SCH";
        }
        if (schoolShortName == null) schoolShortName = "SCH";

        String receiptNumber = String.format("%s-%d-%s-%s-%s",
                schoolShortName,
                LocalDate.now().getYear(),
                LocalDate.now().format(DateTimeFormatter.ofPattern("MMdd")),
                student.getAdmissionNumber(),
                UUID.randomUUID().toString().substring(0, 4).toUpperCase()
        );

        List<MonthlyFeeStructure> structures = monthlyFeeStructureRepository
                .findBySchoolIdAndAcademicYearIdAndClassIdOrderByFeeCategoryId(schoolId, enrollment.getAcademicYearId(), enrollment.getClassId());

        BigDecimal subtotal = BigDecimal.ZERO;
        for (MonthlyFeeStructure s : structures) {
            subtotal = subtotal.add(s.getDefaultAmount());
        }

        BigDecimal previousDues = enrollment.getPreviousDues() != null ? enrollment.getPreviousDues() : BigDecimal.ZERO;
        if (previousDues.compareTo(BigDecimal.ZERO) > 0) {
            subtotal = subtotal.add(previousDues);
        }

        BigDecimal grandTotal = subtotal.subtract(request.getConcessionAmount()).add(request.getLateFee());
        BigDecimal amountPaid = grandTotal; 

        PaymentTransaction transaction = PaymentTransaction.builder()
                .schoolId(schoolId)
                .receiptNumber(receiptNumber)
                .studentId(student.getId())
                .enrollmentId(enrollment.getId())
                .academicYearId(enrollment.getAcademicYearId())
                .collectionMonth(request.getMonth())
                .collectionYear(request.getYear())
                .previousBalance(BigDecimal.ZERO)
                .subtotal(subtotal)
                .concessionApplied(request.getConcessionAmount())
                .lateFee(request.getLateFee())
                .grandTotal(grandTotal)
                .amountPaid(amountPaid)
                .remainingBalance(BigDecimal.ZERO)
                .paymentMethod(request.getPaymentMethod())
                .paymentReference(request.getPaymentReference())
                .paymentDate(LocalDate.now())
                .remarks(request.getRemarks())
                .status("CONFIRMED")
                .collectedBy(collectedByUserId)
                .confirmedAt(Instant.now())
                .build();

        PaymentTransaction savedTransaction = paymentTransactionRepository.save(transaction);

        List<PaymentLineItem> lineItems = structures.stream().map(item ->
                PaymentLineItem.builder()
                        .schoolId(schoolId)
                        .transactionId(savedTransaction.getId())
                        .feeCategoryId(item.getFeeCategoryId())
                        .amountCharged(item.getDefaultAmount())
                        .build()
        ).collect(Collectors.toList());

        paymentLineItemRepository.saveAll(lineItems);

        if (previousDues.compareTo(BigDecimal.ZERO) > 0) {
            PaymentLineItem arrearsItem = PaymentLineItem.builder()
                    .schoolId(schoolId)
                    .transactionId(savedTransaction.getId())
                    .feeCategoryId(getOrCreateArrearsCategoryId(schoolId))
                    .amountCharged(previousDues)
                    .build();
            paymentLineItemRepository.save(arrearsItem);

            enrollment.setPreviousDues(BigDecimal.ZERO);
            studentEnrollmentRepository.save(enrollment);
        }

        return savedTransaction;
    }

    private UUID getCurrentAcademicYearId(UUID schoolId) {
        List<UUID> activeYears = jdbcTemplate.query(
                "SELECT id FROM academic_years WHERE school_id = ? AND is_current = true LIMIT 1",
                (rs, rowNum) -> rs.getObject("id", java.util.UUID.class),
                schoolId
        );
        if (!activeYears.isEmpty()) {
            return activeYears.get(0);
        }
        throw new IllegalStateException("No active academic year found");
    }

    private UUID getOrCreateArrearsCategoryId(UUID schoolId) {
        try {
            List<UUID> ids = jdbcTemplate.query(
                "SELECT id FROM fee_categories WHERE school_id = ? AND name = 'Previous Year Arrears'",
                (rs, rowNum) -> rs.getObject("id", UUID.class),
                schoolId
            );
            if (!ids.isEmpty()) {
                return ids.get(0);
            }
            
            UUID newId = UUID.randomUUID();
            jdbcTemplate.update(
                "INSERT INTO fee_categories (id, school_id, name, display_order, is_active, created_at) VALUES (?, ?, 'Previous Year Arrears', 999, true, NOW())",
                newId, schoolId
            );
            return newId;
        } catch (Exception e) {
            return UUID.fromString("00000000-0000-0000-0000-000000000001");
        }
    }
}
