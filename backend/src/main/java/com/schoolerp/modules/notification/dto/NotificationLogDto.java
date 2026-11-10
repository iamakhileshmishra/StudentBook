package com.schoolerp.modules.notification.dto;

import com.schoolerp.modules.notification.entity.NotificationLog;

import java.time.Instant;

public record NotificationLogDto(String channel, String recipient, String status, String detail, Instant createdAt) {

    public static NotificationLogDto from(NotificationLog log) {
        return new NotificationLogDto(log.getChannel(), log.getRecipient(), log.getStatus(),
                log.getDetail(), log.getCreatedAt());
    }
}
