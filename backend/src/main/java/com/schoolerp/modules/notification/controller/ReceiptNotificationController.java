package com.schoolerp.modules.notification.controller;

import com.schoolerp.modules.fee.entity.PaymentTransaction;
import com.schoolerp.modules.fee.repository.PaymentTransactionRepository;
import com.schoolerp.modules.notification.dto.NotificationLogDto;
import com.schoolerp.modules.notification.repository.NotificationLogRepository;
import com.schoolerp.modules.notification.service.ParentNotificationService;
import com.schoolerp.shared.exception.ResourceNotFoundException;
import com.schoolerp.shared.security.TenantContext;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

/** Staff view of parent notifications for one receipt, and "Resend". */
@RestController
@RequestMapping("/api/payments/receipt/{receiptNumber}/notifications")
@RequiredArgsConstructor
public class ReceiptNotificationController {

    private final PaymentTransactionRepository paymentTransactionRepository;
    private final NotificationLogRepository notificationLogRepository;
    private final ParentNotificationService parentNotificationService;

    @GetMapping
    public ResponseEntity<List<NotificationLogDto>> list(@PathVariable String receiptNumber) {
        PaymentTransaction tx = find(receiptNumber);
        return ResponseEntity.ok(notificationLogRepository.findTop10ByTransactionIdOrderByCreatedAtDesc(tx.getId())
                .stream().map(NotificationLogDto::from).toList());
    }

    /** Sends the parent email now, even if automatic emails are turned off. */
    @PostMapping("/email")
    public ResponseEntity<NotificationLogDto> resendEmail(@PathVariable String receiptNumber) {
        PaymentTransaction tx = find(receiptNumber);
        return ResponseEntity.ok(NotificationLogDto.from(
                parentNotificationService.sendEmail(tx.getSchoolId(), tx.getId(), true)));
    }

    private PaymentTransaction find(String receiptNumber) {
        UUID schoolId = TenantContext.getSchoolId();
        return paymentTransactionRepository.findBySchoolIdAndReceiptNumber(schoolId, receiptNumber)
                .orElseThrow(() -> new ResourceNotFoundException("Receipt not found"));
    }
}
