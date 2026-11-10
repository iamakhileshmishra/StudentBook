package com.schoolerp.modules.notification.repository;

import com.schoolerp.modules.notification.entity.NotificationLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface NotificationLogRepository extends JpaRepository<NotificationLog, UUID> {

    List<NotificationLog> findTop10ByTransactionIdOrderByCreatedAtDesc(UUID transactionId);
}
