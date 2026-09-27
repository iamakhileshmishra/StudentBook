package com.schoolerp.modules.fee.controller;

import com.schoolerp.modules.dashboard.dto.RecentTransactionDto;
import com.schoolerp.modules.fee.dto.FullReceiptDto;
import com.schoolerp.modules.fee.entity.PaymentTransaction;
import com.schoolerp.modules.fee.repository.PaymentTransactionRepository;
import com.schoolerp.modules.fee.service.PaymentHistoryService;
import com.schoolerp.shared.security.TenantContext;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/payments")
@RequiredArgsConstructor
public class PaymentHistoryController {

    private final PaymentHistoryService paymentHistoryService;
    private final PaymentTransactionRepository paymentTransactionRepository;
    private final JdbcTemplate jdbcTemplate;

    @GetMapping("/history")
    public ResponseEntity<Page<RecentTransactionDto>> getHistory(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "50") int size,
            @RequestParam(defaultValue = "") String search) {
        
        UUID schoolId = TenantContext.getSchoolId();
        // BUG-11: negative page / zero or huge size used to 500 or run unbounded queries
        page = Math.max(page, 0);
        size = Math.max(1, Math.min(size, 100));
        
        // Treat % and _ typed by the user literally (Postgres LIKE default escape is \)
        String escaped = search.trim().toLowerCase()
                .replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_");
        String searchParam = "%" + escaped + "%";
        
        String countSql = "SELECT COUNT(*) FROM payment_transactions pt " +
                "JOIN students s ON pt.student_id = s.id " +
                "WHERE pt.school_id = ? AND pt.status = 'CONFIRMED' " +
                "AND (LOWER(pt.receipt_number) LIKE ? OR LOWER(s.first_name) LIKE ? OR LOWER(s.last_name) LIKE ?)";
                
        Long totalElements = jdbcTemplate.queryForObject(countSql, Long.class, schoolId, searchParam, searchParam, searchParam);
        if (totalElements == null) totalElements = 0L;

        String querySql = "SELECT pt.id, pt.receipt_number, pt.amount_paid, pt.payment_method, pt.confirmed_at, " +
                "s.first_name, s.last_name, s.admission_number, c.name as class_name " +
                "FROM payment_transactions pt " +
                "JOIN students s ON pt.student_id = s.id " +
                "JOIN student_enrollments e ON pt.enrollment_id = e.id " +
                "JOIN classes c ON e.class_id = c.id " +
                "WHERE pt.school_id = ? AND pt.status = 'CONFIRMED' " +
                "AND (LOWER(pt.receipt_number) LIKE ? OR LOWER(s.first_name) LIKE ? OR LOWER(s.last_name) LIKE ?) " +
                "ORDER BY pt.confirmed_at DESC " +
                "LIMIT ? OFFSET ?";
                
                List<RecentTransactionDto> content = jdbcTemplate.query(querySql, (rs, rowNum) -> {
            return RecentTransactionDto.builder()
                    .id(UUID.fromString(rs.getString("id")))
                    .receiptNumber(rs.getString("receipt_number"))
                    .studentName(rs.getString("first_name") + " " + rs.getString("last_name"))
                    .admissionNumber(rs.getString("admission_number"))
                    .className(rs.getString("class_name"))
                    .amountPaid(rs.getBigDecimal("amount_paid"))
                    .paymentMethod(rs.getString("payment_method"))
                    .confirmedAt(rs.getTimestamp("confirmed_at").toInstant())
                    .build();
        }, schoolId, searchParam, searchParam, searchParam, size, page * size);

        org.springframework.data.domain.PageImpl<RecentTransactionDto> dtoPage = 
            new org.springframework.data.domain.PageImpl<>(content, PageRequest.of(page, size), totalElements);

        return ResponseEntity.ok(dtoPage);
    }

    @GetMapping("/receipt/{receiptNumber}")
    public ResponseEntity<FullReceiptDto> getReceipt(@PathVariable String receiptNumber) {
        return ResponseEntity.ok(paymentHistoryService.getFullReceipt(receiptNumber));
    }
}
