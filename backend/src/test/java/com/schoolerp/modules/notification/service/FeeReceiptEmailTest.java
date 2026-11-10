package com.schoolerp.modules.notification.service;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;

class FeeReceiptEmailTest {

    private static FeeReceiptEmail.Details details(String school, String section) {
        return new FeeReceiptEmail.Details(school, "Riya Sharma", "Class 5", section,
                11, 2026, "BANK_TRANSFER", LocalDate.of(2026, 11, 3),
                new BigDecimal("1500"), "SNVP-2026-0042", "https://studentbook.onrender.com/r/abc123");
    }

    @Test
    void containsMonthModeDateAndLink() {
        FeeReceiptEmail email = FeeReceiptEmail.build(details("Shanti Niketan", "A"));

        assertThat(email.subject()).isEqualTo("Fee received for November 2026 – Shanti Niketan");
        assertThat(email.text())
                .startsWith("Dear Parent,")
                .contains("Riya Sharma (Class 5 - A)")
                .contains("for the month of November 2026")
                .contains("via Bank transfer on 03/11/2026")
                .contains("₹1500.00")
                .contains("SNVP-2026-0042")
                .contains("https://studentbook.onrender.com/r/abc123")
                .contains("With regards,\nShanti Niketan");
        assertThat(email.html()).contains("href=\"https://studentbook.onrender.com/r/abc123\"");
    }

    @Test
    void omitsSectionWhenClassHasNone() {
        assertThat(FeeReceiptEmail.build(details("School", null)).text()).contains("Riya Sharma (Class 5)");
    }

    @Test
    void escapesHtml() {
        FeeReceiptEmail email = FeeReceiptEmail.build(details("<b>Evil</b> & Co", "A"));
        assertThat(email.html()).contains("&lt;b&gt;Evil&lt;/b&gt; &amp; Co").doesNotContain("<b>Evil</b>");
    }

    @Test
    void namesPaymentModes() {
        assertThat(FeeReceiptEmail.paymentMode("CASH")).isEqualTo("Cash");
        assertThat(FeeReceiptEmail.paymentMode("UPI")).isEqualTo("UPI");
        assertThat(FeeReceiptEmail.paymentMode("CHEQUE")).isEqualTo("Cheque");
        assertThat(FeeReceiptEmail.paymentMode("OTHER")).isEqualTo("OTHER");
    }
}
