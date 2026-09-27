package com.schoolerp.modules.fee.service;

import com.schoolerp.modules.fee.dto.FeeDuesResponse;
import com.schoolerp.modules.fee.dto.FullReceiptDto;
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

        String sectionName = "A";
        try {
            sectionName = jdbcTemplate.queryForObject("SELECT name FROM sections WHERE id = ?", String.class, map.get("section_id"));
        } catch (Exception ignored) {}

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
