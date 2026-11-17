package com.mynx.myca500.patient_forms.service;

import com.mynx.myca500.patient_forms.dto.EmailDto;
import com.mynx.myca500.patient_forms.dto.EmailSentResponse;
import com.mynx.myca500.patient_forms.exception.ApiException;
import com.mynx.myca500.patient_forms.exception.ResourceCreationException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;
import software.amazon.awssdk.core.exception.SdkException;
import software.amazon.awssdk.services.ses.SesClient;
import software.amazon.awssdk.services.ses.model.Body;
import software.amazon.awssdk.services.ses.model.Content;
import software.amazon.awssdk.services.ses.model.Destination;
import software.amazon.awssdk.services.ses.model.Message;
import software.amazon.awssdk.services.ses.model.SendEmailRequest;
import software.amazon.awssdk.services.ses.model.SendEmailResponse;
import software.amazon.awssdk.services.ses.model.SesException;

import java.util.List;
import java.util.Locale;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class EmailService {


    private static final Map<String, String> TEMPLATE_SUBJECTS = Map.of(
            "form-request", "You have a form to complete");

    private final SesClient sesClient;
    private final TemplateEngine templateEngine;
    private final SmtpEmailSender smtpEmailSender;

    /** "smtp" (default) sends via SES SMTP; "ses" uses the SES SDK client. */
    @Value("${app.email.provider:smtp}")
    private String provider;

    @Value("${app.email.from}")
    private String from;

    @Value("${app.environment-url}")
    private String environmentUrl;

    public EmailSentResponse send(EmailDto dto) {
        log.info("email send request: template={}, recipients={}", dto.template(), dto.to().length());
        String subject;
        Body body;
        String rawContent;
        boolean isHtml;

        if (dto.template() != null) {
            subject = TEMPLATE_SUBJECTS.get(dto.template());
            if (subject == null) {
                throw ApiException.badRequest(List.of("Unknown email template: " + dto.template()));
            }
            Context context = new Context(Locale.US);
            context.setVariable("email", dto.values());
            context.setVariable("environmenturl", environmentUrl);
            String html = templateEngine.process(dto.template(), context);
            body = Body.builder().html(content(html)).build();

            rawContent = html;
            log.info("email send request: raw content={}", rawContent);
            isHtml = true;
        } else {
            subject = dto.subject().trim().replaceAll("[\\r\\n]+", " ");
            body = Body.builder().text(content(dto.body())).build();
            rawContent = dto.body();
            isHtml = false;
        }

        if ("smtp".equalsIgnoreCase(provider)) {
            String messageId = smtpEmailSender.send(from, dto.to(), subject, rawContent, isHtml);
            log.info("Email sent: template={}, recipients={}, messageId={}",
                    dto.template(), dto.to().length(), messageId);
            return new EmailSentResponse(messageId);
        }

        SendEmailRequest request = SendEmailRequest.builder()
                .source(from)
                .destination(Destination.builder().toAddresses(dto.to()).build())
                .message(Message.builder().subject(content(subject)).body(body).build())
                .build();

        try {
            SendEmailResponse response = sesClient.sendEmail(request);
            log.info("Email sent: template={}, recipients={}, messageId={}",
                    dto.template(), dto.to().length(), response.messageId());
            return new EmailSentResponse(response.messageId());
        } catch (SesException e) {
            log.error("SES rejected the email: {}", e.awsErrorDetails().errorMessage(), e);
            throw new ResourceCreationException("Failed to send email");
        } catch (SdkException e) {
            log.error("Could not reach SES: {}", e.getMessage(), e);
            throw new ResourceCreationException("Failed to send email");
        }
    }

    private static Content content(String data) {
        return Content.builder().data(data).charset("UTF-8").build();
    }
}
