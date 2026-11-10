package com.schoolerp.modules.fee.controller;

import com.schoolerp.modules.fee.dto.PublicReceiptDto;
import com.schoolerp.modules.fee.service.PaymentHistoryService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.CacheControl;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Receipts parents open from the "fee paid" email. No login; see PaymentHistoryService#getPublicReceipt. */
@RestController
@RequestMapping("/api/public/receipts")
@RequiredArgsConstructor
public class PublicReceiptController {

    private final PaymentHistoryService paymentHistoryService;

    @GetMapping("/{token}")
    public ResponseEntity<PublicReceiptDto> getReceipt(@PathVariable String token) {
        return ResponseEntity.ok()
                .cacheControl(CacheControl.noStore())
                .header("X-Robots-Tag", "noindex")
                .body(paymentHistoryService.getPublicReceipt(token));
    }
}
