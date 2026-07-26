package com.schoolerp.modules.fee.repository;

import com.schoolerp.modules.fee.entity.PaymentLineItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface PaymentLineItemRepository extends JpaRepository<PaymentLineItem, UUID> {
    List<PaymentLineItem> findByTransactionId(UUID transactionId);
    List<PaymentLineItem> findByTransactionIdAndSchoolId(UUID transactionId, UUID schoolId);
}
