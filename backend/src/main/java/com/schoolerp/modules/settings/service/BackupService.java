package com.schoolerp.modules.settings.service;

import com.schoolerp.shared.security.TenantContext;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.UUID;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

@Service
@RequiredArgsConstructor
public class BackupService {

    private final JdbcTemplate jdbcTemplate;
    private final com.schoolerp.shared.time.SchoolClock clock;

    public void generateCsvBackupZip(UUID academicYearId, HttpServletResponse response) throws IOException {
        UUID schoolId = TenantContext.getSchoolId();
        
        String shortName;
        try {
            shortName = jdbcTemplate.queryForObject("SELECT short_name FROM schools WHERE id = ?", String.class, schoolId);
        } catch (Exception e) {
            shortName = "SCHOOL";
        }
        if (shortName == null || shortName.isBlank()) shortName = "SCHOOL";

        // Short name is used in a header; keep it to safe characters
        shortName = shortName.replaceAll("[^A-Za-z0-9_-]", "");
        if (shortName.isEmpty()) shortName = "SCHOOL";
        String dateStr = clock.today().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        String filename = String.format("%s_%s.zip", shortName, dateStr);

        response.setContentType("application/zip");
        response.setHeader("Content-Disposition", "attachment; filename=\"" + filename + "\"");

        try (ZipOutputStream zos = new ZipOutputStream(response.getOutputStream())) {
            String studentQuery;
            Object[] studentArgs;
            if (academicYearId != null) {
                studentQuery = "SELECT s.id, s.admission_number, s.first_name, s.last_name, s.gender, s.status, c.name as class_name, sec.name as section_name, s.father_name, s.primary_contact_phone, e.enrollment_date " +
                    "FROM students s " +
                    "LEFT JOIN student_enrollments e ON s.id = e.student_id AND e.academic_year_id = ? " +
                    "LEFT JOIN classes c ON e.class_id = c.id " +
                    "LEFT JOIN sections sec ON e.section_id = sec.id " +
                    "WHERE s.school_id = ?";
                studentArgs = new Object[]{academicYearId, schoolId};
            } else {
                studentQuery = "SELECT s.id, s.admission_number, s.first_name, s.last_name, s.gender, s.status, c.name as class_name, sec.name as section_name, s.father_name, s.primary_contact_phone, e.enrollment_date " +
                    "FROM students s " +
                    "LEFT JOIN student_enrollments e ON s.id = e.student_id AND e.academic_year_id = (SELECT id FROM academic_years WHERE school_id = ? AND is_current = TRUE LIMIT 1) " +
                    "LEFT JOIN classes c ON e.class_id = c.id " +
                    "LEFT JOIN sections sec ON e.section_id = sec.id " +
                    "WHERE s.school_id = ?";
                studentArgs = new Object[]{schoolId, schoolId};
            }

            // 1. Export Students
            writeCsvStream(zos, "students.csv", 
                new String[]{"ID", "Admission Number", "First Name", "Last Name", "Gender", "Status", "Class", "Section", "Father Name", "Phone", "Enrollment Date"},
                studentQuery,
                studentArgs
            );

            // 2. Export Transactions
            writeCsvStream(zos, "transactions.csv",
                new String[]{"Transaction ID", "Receipt Number", "Student Name", "Admission No", "Collection Month", "Collection Year", "Amount Paid", "Late Fee", "Concession", "Method", "Status", "Date"},
                "SELECT pt.id, pt.receipt_number, CONCAT(s.first_name, ' ', s.last_name), s.admission_number, pt.collection_month, pt.collection_year, pt.amount_paid, pt.late_fee, pt.concession_applied, pt.payment_method, pt.status, pt.payment_date " +
                "FROM payment_transactions pt " +
                "JOIN students s ON pt.student_id = s.id " +
                "WHERE pt.school_id = ? " +
                "ORDER BY pt.confirmed_at DESC",
                schoolId
            );

            // 3. Export Fee Structures
            writeCsvStream(zos, "fee_structures.csv",
                new String[]{"Class", "Fee Category", "Amount"},
                "SELECT c.name as class_name, fc.name as category_name, m.default_amount " +
                "FROM monthly_fee_structures m " +
                "JOIN classes c ON m.class_id = c.id " +
                "JOIN fee_categories fc ON m.fee_category_id = fc.id " +
                "WHERE m.school_id = ? " +
                "ORDER BY c.name, fc.name",
                schoolId
            );
        }
    }

    private void writeCsvStream(ZipOutputStream zos, String fileName, String[] headers, String sql, Object... args) throws IOException {
        zos.putNextEntry(new ZipEntry(fileName));
        PrintWriter writer = new PrintWriter(new OutputStreamWriter(zos, StandardCharsets.UTF_8));
        writer.println(String.join(",", headers));
        
        jdbcTemplate.query(sql, rs -> {
            String[] row = new String[headers.length];
            for (int i = 0; i < headers.length; i++) {
                try {
                    Object val = rs.getObject(i + 1);
                    String str = neutralizeFormula(val == null ? "" : val.toString());
                    if (str.contains(",") || str.contains("\"") || str.contains("\n") || str.contains("\r")) {
                        str = "\"" + str.replace("\"", "\"\"") + "\"";
                    }
                    row[i] = str;
                } catch (Exception e) {
                    row[i] = "";
                }
            }
            writer.println(String.join(",", row));
        }, args);
        
        writer.flush();
        zos.closeEntry();
    }

    /**
     * Spreadsheet apps run cells starting with = + - @ (or tab/CR) as formulas, so a
     * student named =HYPERLINK(...) would execute when the backup is opened
     * (SEC-12). Prefix such cells with an apostrophe so they stay plain text.
     * Plain negative numbers are left alone.
     */
    static String neutralizeFormula(String value) {
        if (value.isEmpty()) return value;
        char first = value.charAt(0);
        boolean risky = first == '=' || first == '+' || first == '@' || first == '\t' || first == '\r'
                || (first == '-' && !value.matches("-\\d+(\\.\\d+)?"));
        return risky ? "'" + value : value;
    }
}
