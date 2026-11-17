package com.mynx.myca500.patient_forms.service;

import com.mynx.myca500.patient_forms.exception.ResourceCreationException;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.mail.MailException;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;

/**
 * Sends email through the Amazon SES SMTP interface (STARTTLS, port 587)
 * using the spring.mail.* settings.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class SmtpEmailSender {

    private final JavaMailSender mailSender;

    /**
     * @param html true if {@code content} is HTML, false for plain text
     * @return the Message-ID header of the sent message
     */
    public String send(String from, String to, String subject, String content, boolean html) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, false, StandardCharsets.UTF_8.name());
            helper.setFrom(from);
            helper.setTo(to);
            helper.setSubject(subject);
            helper.setText(content, html);

            mailSender.send(message);

            String messageId = message.getMessageID();
            log.info("Email sent via SMTP: messageId={}", messageId);
            return messageId;
        } catch (MessagingException | MailException e) {
            log.error("SMTP send failed: {}", e.getMessage(), e);
            throw new ResourceCreationException("Failed to send email");
        }
    }
}
