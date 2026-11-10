package com.schoolerp.modules.notification.service;

import com.schoolerp.modules.fee.entity.PaymentTransaction;
import com.schoolerp.modules.fee.repository.PaymentTransactionRepository;
import com.schoolerp.modules.notification.entity.NotificationLog;
import com.schoolerp.modules.notification.event.FeePaidEvent;
import com.schoolerp.modules.notification.repository.NotificationLogRepository;
import com.schoolerp.shared.exception.BusinessException;
import com.schoolerp.shared.exception.ResourceNotFoundException;
import com.schoolerp.shared.mail.MailService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

/**
 * Tells parents that a fee was paid. Email only for now; WhatsApp will be another channel.
 *
 * Runs on a background thread after the payment commits, so a slow or failing mail
 * provider never delays or breaks fee collection. Every attempt is written to
 * notification_log, including why nothing was sent.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ParentNotificationService {

    static final String NO_EMAIL = "No parent email on file";
    static final String TURNED_OFF = "Parent emails are turned off in Settings";
    static final String CANCELLED = "The receipt was cancelled";

    private final PaymentTransactionRepository paymentTransactionRepository;
    private final NotificationLogRepository notificationLogRepository;
    private final MailService mailService;
    private final JdbcTemplate jdbcTemplate;

    @Value("${app.frontend-url}")
    private String frontendUrl;

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onFeePaid(FeePaidEvent event) {
        try {
            sendEmail(event.schoolId(), event.transactionId(), false);
        } catch (Exception e) {
            log.error("Parent email for transaction {} failed: {}", event.transactionId(), e.getMessage());
        }
    }

    /**
     * Emails the parent about one payment and records the outcome.
     *
     * @param manual true when staff press "Resend"; that ignores the school's on/off switch
     */
    public NotificationLog sendEmail(UUID schoolId, UUID transactionId, boolean manual) {
        PaymentTransaction tx = paymentTransactionRepository.findById(transactionId)
                .filter(t -> t.getSchoolId().equals(schoolId))
                .orElseThrow(() -> new ResourceNotFoundException("Receipt not found"));

        Map<String, Object> school = jdbcTemplate.queryForMap(
                "SELECT name, notify_parent_email FROM schools WHERE id = ?", schoolId);
        Map<String, Object> student = jdbcTemplate.queryForMap("""
                SELECT s.first_name, s.last_name, s.email, c.name AS class_name, sec.name AS section_name
                FROM students s
                JOIN student_enrollments e ON e.id = ?
                JOIN classes c ON c.id = e.class_id
                LEFT JOIN sections sec ON sec.id = e.section_id
                WHERE s.id = ?
                """, tx.getEnrollmentId(), tx.getStudentId());

        String email = trimToNull((String) student.get("email"));

        if (manual && sentRecently(tx.getId())) {
            throw new BusinessException("The receipt was emailed less than a minute ago. Please wait before resending.");
        }
        if (!"CONFIRMED".equals(tx.getStatus())) {
            return record(tx, email, NotificationLog.SKIPPED, CANCELLED);
        }
        if (!manual && !Boolean.TRUE.equals(school.get("notify_parent_email"))) {
            return record(tx, email, NotificationLog.SKIPPED, TURNED_OFF);
        }
        if (email == null) {
            return record(tx, null, NotificationLog.SKIPPED, NO_EMAIL);
        }

        String studentName = (student.get("first_name") + " " + nz((String) student.get("last_name"))).trim();
        FeeReceiptEmail message = FeeReceiptEmail.build(new FeeReceiptEmail.Details(
                (String) school.get("name"), studentName,
                (String) student.get("class_name"), (String) student.get("section_name"),
                tx.getCollectionMonth(), tx.getCollectionYear(), tx.getPaymentMethod(), tx.getPaymentDate(),
                tx.getGrandTotal(), tx.getReceiptNumber(), receiptUrl(tx.getShareToken())));

        try {
            mailService.send(email, message.subject(), message.text(), message.html());
            return record(tx, email, NotificationLog.SENT, null);
        } catch (Exception e) {
            log.warn("Could not email receipt {} to parent: {}", tx.getReceiptNumber(), e.getMessage());
            return record(tx, email, NotificationLog.FAILED, truncate(e.getMessage()));
        }
    }

    private boolean sentRecently(UUID transactionId) {
        return notificationLogRepository.findTop10ByTransactionIdOrderByCreatedAtDesc(transactionId).stream()
                .anyMatch(l -> NotificationLog.SENT.equals(l.getStatus())
                        && l.getCreatedAt() != null
                        && l.getCreatedAt().isAfter(Instant.now().minusSeconds(60)));
    }

    String receiptUrl(String shareToken) {
        String base = frontendUrl.endsWith("/") ? frontendUrl.substring(0, frontendUrl.length() - 1) : frontendUrl;
        return base + "/r/" + shareToken;
    }

    private NotificationLog record(PaymentTransaction tx, String recipient, String status, String detail) {
        return notificationLogRepository.save(NotificationLog.builder()
                .schoolId(tx.getSchoolId())
                .transactionId(tx.getId())
                .channel(NotificationLog.CHANNEL_EMAIL)
                .recipient(recipient)
                .status(status)
                .detail(detail)
                .build());
    }

    private static String trimToNull(String s) {
        return s == null || s.isBlank() ? null : s.trim();
    }

    private static String nz(String s) {
        return s == null ? "" : s;
    }

    private static String truncate(String s) {
        if (s == null) return "Unknown error";
        return s.length() > 500 ? s.substring(0, 500) : s;
    }
}
