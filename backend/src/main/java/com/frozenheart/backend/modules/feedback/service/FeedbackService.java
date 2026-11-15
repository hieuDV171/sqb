package com.frozenheart.backend.modules.feedback.service;

import com.frozenheart.backend.core.constant.ResponseCode;
import com.frozenheart.backend.core.exception.AppException;
import com.frozenheart.backend.modules.feedback.dto.FeedbackRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DataAccessException;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.mail.MailException;
import org.springframework.mail.MailSender;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.stereotype.Service;

import java.time.Duration;

@Service
@Slf4j
public class FeedbackService {
    private static final Duration COOLDOWN = Duration.ofMinutes(1);

    private final MailSender mailSender;
    private final StringRedisTemplate redis;
    private final String fromEmail;
    private final String recipient;

    public FeedbackService(MailSender mailSender, StringRedisTemplate redis,
                           @Value("${spring.mail.username}") String fromEmail,
                           @Value("${app.feedback.recipient}") String recipient) {
        this.mailSender = mailSender;
        this.redis = redis;
        this.fromEmail = fromEmail;
        this.recipient = recipient.strip();
    }

    public void send(long userId, FeedbackRequest request) {
        if (recipient.isBlank()) {
            throw new AppException(ResponseCode.FEEDBACK_UNAVAILABLE);
        }

        try {
            // Keep the cooldown even if SMTP fails, including when delivery is uncertain.
            Boolean acquired = redis.opsForValue().setIfAbsent("sqb:feedback:cooldown:" + userId, "1", COOLDOWN);
            if (Boolean.FALSE.equals(acquired)) {
                throw new AppException(ResponseCode.FEEDBACK_RATE_LIMITED);
            }
            if (acquired == null) {
                throw new AppException(ResponseCode.FEEDBACK_UNAVAILABLE);
            }

            SimpleMailMessage mail = new SimpleMailMessage();
            mail.setFrom(fromEmail);
            mail.setTo(recipient);
            mail.setSubject("[SQB] Góp ý về web client");
            mail.setText("""
                    ID tài khoản SQB: %d
                    Màn hình: %s

                    %s
                    """.formatted(userId, request.pagePath() == null ? "Không có" : request.pagePath(),
                    request.message().strip()));
            mailSender.send(mail);
        } catch (DataAccessException | MailException ex) {
            log.warn("Không thể gửi góp ý SQB cho tài khoản {} ({})", userId, ex.getClass().getSimpleName());
            throw new AppException(ResponseCode.FEEDBACK_UNAVAILABLE);
        }
    }
}
