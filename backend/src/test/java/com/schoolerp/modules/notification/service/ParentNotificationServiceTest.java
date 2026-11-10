package com.schoolerp.modules.notification.service;

import com.schoolerp.modules.fee.entity.PaymentTransaction;
import com.schoolerp.modules.fee.repository.PaymentTransactionRepository;
import com.schoolerp.modules.notification.entity.NotificationLog;
import com.schoolerp.modules.notification.repository.NotificationLogRepository;
import com.schoolerp.shared.exception.BusinessException;
import com.schoolerp.shared.exception.ResourceNotFoundException;
import com.schoolerp.shared.mail.MailService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mail.MailSendException;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class ParentNotificationServiceTest {

    private static final UUID SCHOOL = UUID.randomUUID();
    private static final UUID OTHER_SCHOOL = UUID.randomUUID();

    @Mock PaymentTransactionRepository paymentTransactionRepository;
    @Mock NotificationLogRepository notificationLogRepository;
    @Mock MailService mailService;
    @Mock JdbcTemplate jdbcTemplate;

    ParentNotificationService service;
    PaymentTransaction tx;
    Map<String, Object> school;
    Map<String, Object> student;

    @BeforeEach
    void setUp() {
        service = new ParentNotificationService(paymentTransactionRepository, notificationLogRepository,
                mailService, jdbcTemplate);
        ReflectionTestUtils.setField(service, "frontendUrl", "https://app.example/");

        tx = PaymentTransaction.builder()
                .id(UUID.randomUUID()).schoolId(SCHOOL).studentId(UUID.randomUUID()).enrollmentId(UUID.randomUUID())
                .receiptNumber("SNVP-2026-0001").shareToken("tok_" + "a".repeat(40))
                .collectionMonth(11).collectionYear(2026).paymentMethod("UPI")
                .paymentDate(LocalDate.of(2026, 11, 3)).grandTotal(new BigDecimal("1200")).status("CONFIRMED")
                .build();
        when(paymentTransactionRepository.findById(tx.getId())).thenReturn(Optional.of(tx));

        school = new HashMap<>(Map.of("name", "Shanti Niketan", "notify_parent_email", true));
        student = new HashMap<>(Map.of("first_name", "Riya", "last_name", "Sharma",
                "email", "  parent@example.com ", "class_name", "Class 5"));
        when(jdbcTemplate.queryForMap(anyString(), any(Object[].class))).thenAnswer(inv ->
                ((String) inv.getArgument(0)).contains("FROM schools") ? school : student);

        when(notificationLogRepository.save(any())).thenAnswer(inv -> {
            NotificationLog log = inv.getArgument(0);
            log.setCreatedAt(Instant.now());
            return log;
        });
    }

    @Test
    void emailsParentAndLogsSent() {
        NotificationLog log = service.sendEmail(SCHOOL, tx.getId(), false);

        ArgumentCaptor<String> text = ArgumentCaptor.forClass(String.class);
        verify(mailService).send(eq("parent@example.com"), contains("November 2026"), text.capture(), anyString());
        assertThat(text.getValue()).contains("https://app.example/r/" + tx.getShareToken());
        assertThat(log.getStatus()).isEqualTo(NotificationLog.SENT);
        assertThat(log.getRecipient()).isEqualTo("parent@example.com");
        assertThat(log.getChannel()).isEqualTo(NotificationLog.CHANNEL_EMAIL);
    }

    @Test
    void skipsWhenNoParentEmail() {
        student.put("email", "   ");

        NotificationLog log = service.sendEmail(SCHOOL, tx.getId(), false);

        verifyNoInteractions(mailService);
        assertThat(log.getStatus()).isEqualTo(NotificationLog.SKIPPED);
        assertThat(log.getDetail()).isEqualTo(ParentNotificationService.NO_EMAIL);
    }

    @Test
    void automaticEmailRespectsSchoolSwitchButResendDoesNot() {
        school.put("notify_parent_email", false);

        NotificationLog auto = service.sendEmail(SCHOOL, tx.getId(), false);
        assertThat(auto.getStatus()).isEqualTo(NotificationLog.SKIPPED);
        assertThat(auto.getDetail()).isEqualTo(ParentNotificationService.TURNED_OFF);
        verifyNoInteractions(mailService);

        NotificationLog manual = service.sendEmail(SCHOOL, tx.getId(), true);
        assertThat(manual.getStatus()).isEqualTo(NotificationLog.SENT);
    }

    @Test
    void cancelledReceiptIsNotEmailed() {
        tx.setStatus("CANCELLED");

        NotificationLog log = service.sendEmail(SCHOOL, tx.getId(), true);

        verifyNoInteractions(mailService);
        assertThat(log.getDetail()).isEqualTo(ParentNotificationService.CANCELLED);
    }

    @Test
    void mailFailureIsLoggedNotThrown() {
        doThrow(new MailSendException("Brevo returned HTTP 401")).when(mailService)
                .send(anyString(), anyString(), anyString(), anyString());

        NotificationLog log = service.sendEmail(SCHOOL, tx.getId(), false);

        assertThat(log.getStatus()).isEqualTo(NotificationLog.FAILED);
        assertThat(log.getDetail()).contains("HTTP 401");
    }

    @Test
    void backgroundListenerNeverThrows() {
        when(paymentTransactionRepository.findById(any())).thenThrow(new RuntimeException("db down"));

        service.onFeePaid(new com.schoolerp.modules.notification.event.FeePaidEvent(SCHOOL, tx.getId()));

        verifyNoInteractions(mailService);
    }

    @Test
    void cannotNotifyAnotherSchoolsReceipt() {
        assertThatThrownBy(() -> service.sendEmail(OTHER_SCHOOL, tx.getId(), true))
                .isInstanceOf(ResourceNotFoundException.class);
        verifyNoInteractions(mailService);
    }

    @Test
    void resendIsThrottledRightAfterASuccessfulSend() {
        NotificationLog recent = NotificationLog.builder()
                .status(NotificationLog.SENT).createdAt(Instant.now().minusSeconds(10)).build();
        when(notificationLogRepository.findTop10ByTransactionIdOrderByCreatedAtDesc(tx.getId()))
                .thenReturn(List.of(recent));

        assertThatThrownBy(() -> service.sendEmail(SCHOOL, tx.getId(), true))
                .isInstanceOf(BusinessException.class);
        verifyNoInteractions(mailService);
    }
}
