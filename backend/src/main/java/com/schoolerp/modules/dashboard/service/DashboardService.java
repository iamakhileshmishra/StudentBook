package com.schoolerp.modules.dashboard.service;

import com.schoolerp.modules.dashboard.dto.ClassWiseStatDto;
import com.schoolerp.modules.dashboard.dto.DashboardStudentDto;
import com.schoolerp.modules.dashboard.dto.DashboardSummaryDto;
import com.schoolerp.modules.dashboard.dto.RecentTransactionDto;
import com.schoolerp.modules.fee.entity.PaymentTransaction;
import com.schoolerp.modules.fee.repository.PaymentTransactionRepository;
import com.schoolerp.modules.student.repository.StudentEnrollmentRepository;
import com.schoolerp.shared.security.TenantContext;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class DashboardService {

    private final PaymentTransactionRepository paymentTransactionRepository;
    private final StudentEnrollmentRepository studentEnrollmentRepository;
    private final JdbcTemplate jdbcTemplate;
    private final com.schoolerp.shared.time.SchoolClock clock;

    public DashboardSummaryDto getSummary(int month, int year) {
        UUID schoolId = TenantContext.getSchoolId();
        UUID academicYearId = getCurrentAcademicYearId(schoolId);

        BigDecimal todayCollection = paymentTransactionRepository.getTotalCollectionToday(schoolId, clock.today());
        BigDecimal thisMonthCollection = paymentTransactionRepository.getTotalCollectionForMonth(schoolId, month, year);

        long totalStudents = studentEnrollmentRepository.countBySchoolIdAndAcademicYearIdAndStatus(schoolId, academicYearId, "ACTIVE");
        
        String paidSql = """
            SELECT COUNT(DISTINCT pt.student_id)
            FROM payment_transactions pt
            JOIN student_enrollments e ON e.student_id = pt.student_id AND e.academic_year_id = ? AND e.status = 'ACTIVE'
            WHERE pt.school_id = ? AND pt.collection_month = ? AND pt.collection_year = ? AND pt.status = 'CONFIRMED' AND pt.remaining_balance <= 0
        """;
        Long studentsPaidObj = jdbcTemplate.queryForObject(paidSql, Long.class, academicYearId, schoolId, month, year);
        long studentsPaid = studentsPaidObj != null ? studentsPaidObj : 0L;

        String partialSql = """
            SELECT COUNT(DISTINCT pt.student_id)
            FROM payment_transactions pt
            JOIN student_enrollments e ON e.student_id = pt.student_id AND e.academic_year_id = ? AND e.status = 'ACTIVE'
            WHERE pt.school_id = ? AND pt.collection_month = ? AND pt.collection_year = ? AND pt.status = 'CONFIRMED' AND pt.remaining_balance > 0
            AND pt.student_id NOT IN (
                SELECT pt2.student_id FROM payment_transactions pt2
                WHERE pt2.school_id = ? AND pt2.collection_month = ? AND pt2.collection_year = ? AND pt2.status = 'CONFIRMED' AND pt2.remaining_balance <= 0
            )
        """;
        Long studentsPartiallyPaidObj = jdbcTemplate.queryForObject(partialSql, Long.class, academicYearId, schoolId, month, year, schoolId, month, year);
        long studentsPartiallyPaid = studentsPartiallyPaidObj != null ? studentsPartiallyPaidObj : 0L;
        
        long studentsNotPaid = totalStudents - studentsPaid - studentsPartiallyPaid;
        if (studentsNotPaid < 0) studentsNotPaid = 0;

        return DashboardSummaryDto.builder()
                .todayCollection(todayCollection)
                .thisMonthCollection(thisMonthCollection)
                .totalStudents(totalStudents)
                .studentsPaid(studentsPaid)
                .studentsPartiallyPaid(studentsPartiallyPaid)
                .studentsNotPaid(studentsNotPaid)
                .build();
    }

    public List<ClassWiseStatDto> getClassWiseStats(int month, int year) {
        UUID schoolId = TenantContext.getSchoolId();
        UUID academicYearId = getCurrentAcademicYearId(schoolId);

        String sql = """
            SELECT c.id as class_id, c.name as class_name,
                   COUNT(DISTINCT e.student_id) as total_students,
                   COALESCE(SUM(pt.amount_paid), 0) as collected,
                   COALESCE(SUM(pt.remaining_balance), 0) as pending
            FROM classes c
            LEFT JOIN student_enrollments e ON e.class_id = c.id AND e.academic_year_id = ? AND e.status = 'ACTIVE'
            LEFT JOIN payment_transactions pt ON pt.student_id = e.student_id 
                   AND pt.collection_month = ? AND pt.collection_year = ? AND pt.status = 'CONFIRMED'
            WHERE c.school_id = ?
            GROUP BY c.id, c.name, c.numeric_order
            ORDER BY c.numeric_order ASC
        """;

        return jdbcTemplate.query(sql, (rs, rowNum) -> ClassWiseStatDto.builder()
                .classId(rs.getString("class_id"))
                .className(rs.getString("class_name"))
                .totalStudents(rs.getLong("total_students"))
                .collected(rs.getBigDecimal("collected"))
                .pending(rs.getBigDecimal("pending"))
                .build(),
                academicYearId, month, year, schoolId
        );
    }

    public List<RecentTransactionDto> getRecentTransactions(int limit) {
        UUID schoolId = TenantContext.getSchoolId();
        limit = Math.max(1, Math.min(limit, 50)); // BUG-11: negative/huge limits used to 500
        List<PaymentTransaction> transactions = paymentTransactionRepository
                .findBySchoolIdAndStatusOrderByConfirmedAtDesc(schoolId, "CONFIRMED", PageRequest.of(0, limit))
                .getContent();

        // Need student and class name
        return transactions.stream().map(pt -> {
            String studentName = "Unknown";
            String className = "Unknown";
            String admissionNumber = "";
            try {
                var map = jdbcTemplate.queryForMap(
                        "SELECT s.first_name, s.last_name, s.admission_number, c.name as class_name " +
                        "FROM students s " +
                        "JOIN student_enrollments e ON e.student_id = s.id " +
                        "JOIN classes c ON c.id = e.class_id " +
                        "WHERE s.id = ? AND e.id = ?", 
                        pt.getStudentId(), pt.getEnrollmentId()
                );
                studentName = map.get("first_name") + " " + map.get("last_name");
                admissionNumber = (String) map.get("admission_number");
                className = (String) map.get("class_name");
            } catch (Exception ignored) {}

            return RecentTransactionDto.builder()
                    .id(pt.getId())
                    .receiptNumber(pt.getReceiptNumber())
                    .studentName(studentName)
                    .admissionNumber(admissionNumber)
                    .className(className)
                    .amountPaid(pt.getAmountPaid())
                    .paymentMethod(pt.getPaymentMethod())
                    .confirmedAt(pt.getConfirmedAt())
                    .build();
        }).collect(Collectors.toList());
    }

    public List<DashboardStudentDto> getClassStudents(UUID classId, int month, int year) {
        UUID schoolId = TenantContext.getSchoolId();
        UUID academicYearId = getCurrentAcademicYearId(schoolId);

        String sql = """
            SELECT s.id as student_id, s.first_name, s.last_name, s.admission_number,
                   COALESCE(pt.amount_paid, 0) as amount_paid,
                   pt.remaining_balance,
                   pt.id as transaction_id
            FROM students s
            JOIN student_enrollments e ON e.student_id = s.id AND e.academic_year_id = ? AND e.status = 'ACTIVE'
            LEFT JOIN payment_transactions pt ON pt.student_id = s.id 
                   AND pt.collection_month = ? AND pt.collection_year = ? AND pt.status = 'CONFIRMED'
            WHERE s.school_id = ? AND e.class_id = ?
            ORDER BY s.first_name ASC
        """;

        return jdbcTemplate.query(sql, (rs, rowNum) -> {
            String name = rs.getString("first_name") + " " + rs.getString("last_name");
            String admissionNumber = rs.getString("admission_number");
            BigDecimal amountPaid = rs.getBigDecimal("amount_paid");
            BigDecimal remainingBalance = rs.getBigDecimal("remaining_balance");
            String transactionId = rs.getString("transaction_id");

            String status = "UNPAID";
            if (transactionId != null) {
                if (remainingBalance != null && remainingBalance.compareTo(BigDecimal.ZERO) <= 0) {
                    status = "PAID";
                } else {
                    status = "PARTIAL";
                }
            }

            return DashboardStudentDto.builder()
                    .studentId(rs.getString("student_id"))
                    .name(name)
                    .admissionNumber(admissionNumber)
                    .paymentStatus(status)
                    .amountPaid(amountPaid)
                    .build();
        }, academicYearId, month, year, schoolId, classId);
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
}
