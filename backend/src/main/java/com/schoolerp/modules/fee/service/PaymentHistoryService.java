package com.schoolerp.modules.fee.service;

import com.schoolerp.modules.fee.dto.FeeDuesResponse;
import com.schoolerp.modules.fee.dto.FullReceiptDto;
import com.schoolerp.modules.fee.dto.PublicReceiptDto;
import com.schoolerp.modules.fee.entity.PaymentLineItem;
import com.schoolerp.modules.fee.entity.PaymentTransaction;
import com.schoolerp.modules.fee.repository.FeeCategoryRepository;
import com.schoolerp.modules.fee.repository.PaymentLineItemRepository;
import com.schoolerp.modules.fee.repository.PaymentTransactionRepository;
import com.schoolerp.modules.student.dto.StudentSummaryDto;
import com.schoolerp.shared.security.TenantContext;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class PaymentHistoryService {

    private final PaymentTransactionRepository paymentTransactionRepository;
    private final PaymentLineItemRepository paymentLineItemRepository;
    private final FeeCategoryRepository feeCategoryRepository;
    private final JdbcTemplate jdbcTemplate;

    public FullReceiptDto getFullReceipt(String receiptNumber) {
        UUID schoolId = TenantContext.getSchoolId();

        PaymentTransaction transaction = paymentTransactionRepository
                .findBySchoolIdAndReceiptNumber(schoolId, receiptNumber)
                .orElseThrow(() -> new com.schoolerp.shared.exception.ResourceNotFoundException("Receipt not found"));

        return buildFullReceipt(transaction);
    }

    /**
     * The receipt a parent opens from the emailed link. No login: the random share token
     * is the only key, and the response leaves out internal IDs, remarks and payment references.
     */
    public PublicReceiptDto getPublicReceipt(String shareToken) {
        PaymentTransaction transaction = (ShareTokens.looksValid(shareToken)
                ? paymentTransactionRepository.findByShareToken(shareToken)
                : java.util.Optional.<PaymentTransaction>empty())
                .orElseThrow(() -> new com.schoolerp.shared.exception.ResourceNotFoundException("Receipt not found"));

        FullReceiptDto full = buildFullReceipt(transaction);
        StudentSummaryDto student = full.getStudent();

        PublicReceiptDto.School school = jdbcTemplate.queryForObject(
                "SELECT name, address_line1, address_line2, city, state, pincode, phone, email, logo_url FROM schools WHERE id = ?",
                (rs, rowNum) -> new PublicReceiptDto.School(
                        rs.getString("name"), rs.getString("address_line1"), rs.getString("address_line2"),
                        rs.getString("city"), rs.getString("state"), rs.getString("pincode"),
                        rs.getString("phone"), rs.getString("email"), rs.getString("logo_url")),
                transaction.getSchoolId());

        return new PublicReceiptDto(
                school,
                transaction.getReceiptNumber(),
                transaction.getStatus(),
                transaction.getCollectionMonth(),
                transaction.getCollectionYear(),
                transaction.getPaymentDate(),
                transaction.getPaymentMethod(),
                new PublicReceiptDto.Student(student.getFirstName(), student.getLastName(),
                        student.getAdmissionNumber(), student.getFatherName(),
                        student.getClassName(), student.getSectionName()),
                full.getDues().getLineItems().stream()
                        .map(i -> new PublicReceiptDto.LineItem(i.getFeeCategoryName(), i.getAmount()))
                        .toList(),
                transaction.getLateFee(),
                transaction.getConcessionApplied(),
                transaction.getGrandTotal());
    }

    private FullReceiptDto buildFullReceipt(PaymentTransaction transaction) {
        UUID schoolId = transaction.getSchoolId();

        List<PaymentLineItem> items = paymentLineItemRepository.findByTransactionId(transaction.getId());
        
        var categories = feeCategoryRepository.findBySchoolIdAndActiveTrueOrderByDisplayOrderAsc(schoolId)
                .stream().collect(Collectors.toMap(c -> c.getId(), c -> c.getName()));

        List<FeeDuesResponse.LineItem> responseItems = items.stream().map(i -> 
            FeeDuesResponse.LineItem.builder()
                .feeCategoryId(i.getFeeCategoryId())
                .feeCategoryName(categories.getOrDefault(i.getFeeCategoryId(), "Fee"))
                .amount(i.getAmountCharged())
                .build()
        ).collect(Collectors.toList());

        FeeDuesResponse dues = FeeDuesResponse.builder()
                .subtotal(transaction.getSubtotal())
                .expectedConcession(transaction.getConcessionApplied())
                .expectedLateFee(transaction.getLateFee())
                .lineItems(responseItems)
                .build();

        // Get student info using JdbcTemplate
        var map = jdbcTemplate.queryForMap(
                "SELECT s.id, s.first_name, s.last_name, s.admission_number, s.father_name, c.name as class_name, e.section_id " +
                "FROM students s " +
                "JOIN student_enrollments e ON e.student_id = s.id " +
                "JOIN classes c ON c.id = e.class_id " +
                "WHERE s.id = ? AND e.id = ?",
                transaction.getStudentId(), transaction.getEnrollmentId()
        );

        // No section when the class has none (V15); previously this silently defaulted to "A"
        String sectionName = null;
        if (map.get("section_id") != null) {
            sectionName = jdbcTemplate.query("SELECT name FROM sections WHERE id = ?",
                    rs -> rs.next() ? rs.getString(1) : null, map.get("section_id"));
        }

        StudentSummaryDto student = StudentSummaryDto.builder()
                .id((UUID) map.get("id"))
                .firstName((String) map.get("first_name"))
                .lastName((String) map.get("last_name"))
                .admissionNumber((String) map.get("admission_number"))
                .fatherName((String) map.get("father_name"))
                .className((String) map.get("class_name"))
                .sectionName(sectionName)
                .build();

        return FullReceiptDto.builder()
                .receipt(transaction)
                .student(student)
                .dues(dues)
                .lateFee(transaction.getLateFee())
                .concession(transaction.getConcessionApplied())
                .grandTotal(transaction.getGrandTotal())
                .build();
    }
}
