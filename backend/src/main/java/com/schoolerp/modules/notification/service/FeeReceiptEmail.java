package com.schoolerp.modules.notification.service;

import org.springframework.web.util.HtmlUtils;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

/** The "fee paid" email sent to a parent. Pure formatting, no I/O. */
public record FeeReceiptEmail(String subject, String text, String html) {

    private static final DateTimeFormatter MONTH = DateTimeFormatter.ofPattern("MMMM yyyy", Locale.ENGLISH);
    private static final DateTimeFormatter DATE = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    public record Details(String schoolName, String studentName, String className, String sectionName,
                          int month, int year, String paymentMethod, LocalDate paymentDate,
                          BigDecimal amount, String receiptNumber, String receiptUrl) {}

    public static FeeReceiptEmail build(Details d) {
        String period = YearMonth.of(d.year(), d.month()).format(MONTH);
        String date = d.paymentDate().format(DATE);
        String mode = paymentMode(d.paymentMethod());
        String amount = "₹" + d.amount().setScale(2, java.math.RoundingMode.HALF_UP).toPlainString();
        String klass = d.sectionName() == null || d.sectionName().isBlank()
                ? d.className() : d.className() + " - " + d.sectionName();

        String subject = "Fee received for " + period + " – " + d.schoolName();

        String text = """
                Dear Parent,

                The fee of %s (%s) for the month of %s was successfully paid via %s on %s.

                Amount: %s
                Receipt No: %s

                You can view and download the fee receipt here:
                %s

                Thanks,
                With regards,
                %s
                """.formatted(d.studentName(), klass, period, mode, date, amount, d.receiptNumber(),
                d.receiptUrl(), d.schoolName());

        String html = """
                <div style="font-family:Arial,sans-serif;max-width:520px;margin:0 auto;color:#0f172a">
                  <p>Dear Parent,</p>
                  <p>The fee of <strong>%s</strong> (%s) for the month of <strong>%s</strong>
                     was successfully paid via <strong>%s</strong> on <strong>%s</strong>.</p>
                  <table style="border-collapse:collapse;margin:16px 0">
                    <tr><td style="padding:6px 16px 6px 0;color:#64748b">Amount</td>
                        <td style="padding:6px 0"><strong>%s</strong></td></tr>
                    <tr><td style="padding:6px 16px 6px 0;color:#64748b">Receipt No</td>
                        <td style="padding:6px 0;font-family:monospace">%s</td></tr>
                  </table>
                  <p><a href="%s" style="display:inline-block;background:#1a4fd6;color:#fff;padding:10px 20px;
                        border-radius:8px;text-decoration:none">View fee receipt</a></p>
                  <p>Thanks,<br>With regards,<br><strong>%s</strong></p>
                </div>
                """.formatted(esc(d.studentName()), esc(klass), period, esc(mode), date, amount,
                esc(d.receiptNumber()), esc(d.receiptUrl()), esc(d.schoolName()));

        return new FeeReceiptEmail(subject, text, html);
    }

    static String paymentMode(String method) {
        if (method == null) return "—";
        return switch (method) {
            case "CASH" -> "Cash";
            case "UPI" -> "UPI";
            case "CHEQUE" -> "Cheque";
            case "BANK_TRANSFER" -> "Bank transfer";
            default -> method;
        };
    }

    private static String esc(String s) {
        return s == null ? "" : HtmlUtils.htmlEscape(s);
    }
}
