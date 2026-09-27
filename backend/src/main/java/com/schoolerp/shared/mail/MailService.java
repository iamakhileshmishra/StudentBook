package com.schoolerp.shared.mail;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.MailSendException;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class MailService {

    private final JavaMailSender mailSender;

    @Value("${app.mail.from}")
    private String from;

    /**
     * Sends in the background; failures are only logged. Used where the caller's
     * response must not depend on (or take as long as) SMTP, e.g. forgot-password.
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
}
