package com.schoolerp.modules.notification.event;

import java.util.UUID;

/**
 * Published when a fee payment is saved. Listeners run after the transaction commits,
 * so a rolled-back payment never notifies anyone.
 */
public record FeePaidEvent(UUID schoolId, UUID transactionId) {}
