package com.schoolerp.shared.time;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.YearMonth;
import java.time.ZoneId;

/**
 * "Today" for business logic (receipt dates, days late, today's collection).
 * The server runs in UTC, which put everything on yesterday's date between
 * 00:00 and 05:30 IST (test&bug.md BUG-12).
 */
@Component
public class SchoolClock {

    private final ZoneId zone;

    public SchoolClock(@Value("${app.timezone:Asia/Kolkata}") String zone) {
        this.zone = ZoneId.of(zone);
    }

    public LocalDate today() {
        return LocalDate.now(zone);
    }

    public YearMonth currentMonth() {
        return YearMonth.now(zone);
    }

    public ZoneId zone() {
        return zone;
    }
}
