package com.schoolerp.shared.mail;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeMessage;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.MailSendException;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.List;
import java.util.Map;

/**
 * Sends email over one of two transports:
 * - Brevo's HTTPS API when BREVO_API_KEY is set. Used in hosting: Render's free
 *   plan blocks outbound SMTP ports (25/465/587), but HTTPS is allowed.
 * - SMTP otherwise (local development with Mailpit, or any SMTP server).
 */
@Slf4j
@Service
public class MailService {

    private static final URI BREVO_ENDPOINT = URI.create("https://api.brevo.com/v3/smtp/email");

    private final JavaMailSender mailSender;
    private final ObjectMapper objectMapper;
    private final String from;
    private final String brevoApiKey;
    private final HttpClient http = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(10)).build();

    public MailService(JavaMailSender mailSender, ObjectMapper objectMapper,
                       @Value("${app.mail.from}") String from,
                       @Value("${app.mail.brevo-api-key:}") String brevoApiKey) {
        this.mailSender = mailSender;
        this.objectMapper = objectMapper;
        this.from = from;
        this.brevoApiKey = brevoApiKey == null ? "" : brevoApiKey.trim();
        log.info("Email transport: {}", this.brevoApiKey.isEmpty() ? "SMTP" : "Brevo HTTPS API");
    }

    /**
     * Sends in the background; failures are only logged. Used where the caller's
     * response must not depend on (or take as long as) the mail provider, e.g. forgot-password.
     */
    @Async
    public void sendAsync(String to, String subject, String text, String html) {
        try {
            send(to, subject, text, html);
        } catch (Exception e) {
            log.error("Background email to {} failed: {}", to, e.getMessage());
        }
    }

    /** Sends a multipart (plain text + HTML) email. Throws MailException on failure. */
    public void send(String to, String subject, String text, String html) {
        if (brevoApiKey.isEmpty()) {
            sendSmtp(to, subject, text, html);
        } else {
            sendBrevo(to, subject, text, html);
        }
    }

    private void sendSmtp(String to, String subject, String text, String html) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
            helper.setFrom(from);
            helper.setTo(to);
            helper.setSubject(subject);
            helper.setText(text, html);
            mailSender.send(message);
        } catch (MessagingException e) {
            throw new MailSendException("Failed to build email", e);
        }
    }

    private void sendBrevo(String to, String subject, String text, String html) {
        try {
            InternetAddress sender = new InternetAddress(from, true);
            Map<String, Object> body = Map.of(
                    "sender", sender.getPersonal() == null
                            ? Map.of("email", sender.getAddress())
                            : Map.of("email", sender.getAddress(), "name", sender.getPersonal()),
                    "to", List.of(Map.of("email", to)),
                    "subject", subject,
                    "htmlContent", html,
                    "textContent", text);

            HttpRequest request = HttpRequest.newBuilder(BREVO_ENDPOINT)
                    .timeout(Duration.ofSeconds(20))
                    .header("api-key", brevoApiKey)
                    .header("accept", "application/json")
                    .header("content-type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(objectMapper.writeValueAsString(body)))
                    .build();

            HttpResponse<String> response = http.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() / 100 != 2) {
                // Brevo explains the problem (unverified sender, bad key, ...) in the body
                throw new MailSendException("Brevo returned HTTP " + response.statusCode() + ": " + response.body());
            }
        } catch (MailSendException e) {
            throw e;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new MailSendException("Interrupted while sending email", e);
        } catch (Exception e) {
            throw new MailSendException("Failed to send email via Brevo: " + e.getMessage(), e);
        }
    }
}
