package com.schoolerp.modules.fee.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "payment_line_items")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class PaymentLineItem {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "school_id", nullable = false)
    private UUID schoolId;

    @Column(name = "transaction_id", nullable = false)
    private UUID transactionId;

    @Column(name = "fee_category_id", nullable = false)
    private UUID feeCategoryId;

    @Column(name = "amount_charged", nullable = false, precision = 10, scale = 2)
    private BigDecimal amountCharged;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @PrePersist
    public void prePersist() {
        if (amountCharged == null) amountCharged = BigDecimal.ZERO;
    }
}
