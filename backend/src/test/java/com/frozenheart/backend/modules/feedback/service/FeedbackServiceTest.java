package com.frozenheart.backend.modules.feedback.service;

import com.frozenheart.backend.core.constant.ResponseCode;
import com.frozenheart.backend.core.exception.AppException;
import com.frozenheart.backend.modules.feedback.dto.FeedbackRequest;
import jakarta.validation.Validation;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.mail.MailSendException;
import org.springframework.mail.MailSender;
import org.springframework.mail.SimpleMailMessage;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class FeedbackServiceTest {
    private static final long USER_ID = 42L;
    private static final String COOLDOWN_KEY = "sqb:feedback:cooldown:42";
    private static final FeedbackRequest REQUEST = new FeedbackRequest("  Tìm kiếm bị treo  ", "/search");

    @Mock private MailSender mailSender;
    @Mock private StringRedisTemplate redis;
    @Mock private ValueOperations<String, String> values;
    private FeedbackService service;

    @BeforeEach
    void setUp() {
        service = new FeedbackService(mailSender, redis, "web@example.com", "owner@example.com");
    }

    @Test
    void sendsPlainTextToConfiguredRecipientAndThrottlesRepeat() {
        when(redis.opsForValue()).thenReturn(values);
        when(values.setIfAbsent(COOLDOWN_KEY, "1", Duration.ofMinutes(1))).thenReturn(true, false);

        service.send(USER_ID, REQUEST);
        assertThatExceptionOfType(AppException.class).isThrownBy(() -> service.send(USER_ID, REQUEST))
                .extracting(AppException::getResponseCode).isEqualTo(ResponseCode.FEEDBACK_RATE_LIMITED);

        var sent = ArgumentCaptor.forClass(SimpleMailMessage.class);
        verify(mailSender).send(sent.capture());
        assertThat(sent.getValue().getFrom()).isEqualTo("web@example.com");
        assertThat(sent.getValue().getTo()).containsExactly("owner@example.com");
        assertThat(sent.getValue().getText()).isEqualTo("ID tài khoản SQB: 42\nMàn hình: /search\n\nTìm kiếm bị treo\n");
    }

    @Test
    void reportsSmtpFailureWithoutClaimingSuccess() {
        when(redis.opsForValue()).thenReturn(values);
        when(values.setIfAbsent(COOLDOWN_KEY, "1", Duration.ofMinutes(1))).thenReturn(true);
        doThrow(new MailSendException("test failure")).when(mailSender).send(any(SimpleMailMessage.class));

        assertThatExceptionOfType(AppException.class).isThrownBy(() -> service.send(USER_ID, REQUEST))
                .extracting(AppException::getResponseCode).isEqualTo(ResponseCode.FEEDBACK_UNAVAILABLE);
    }

    @Test
    void doesNotSendIfRedisIsUnavailable() {
        when(redis.opsForValue()).thenThrow(new DataAccessResourceFailureException("test failure"));

        assertThatExceptionOfType(AppException.class).isThrownBy(() -> service.send(USER_ID, REQUEST))
                .extracting(AppException::getResponseCode).isEqualTo(ResponseCode.FEEDBACK_UNAVAILABLE);
        verifyNoInteractions(mailSender);
    }

    @Test
    void doesNotSendIfRecipientIsMissing() {
        var unconfigured = new FeedbackService(mailSender, redis, "web@example.com", " ");

        assertThatExceptionOfType(AppException.class).isThrownBy(() -> unconfigured.send(USER_ID, REQUEST))
                .extracting(AppException::getResponseCode).isEqualTo(ResponseCode.FEEDBACK_UNAVAILABLE);
        verifyNoInteractions(redis, mailSender);
    }

    @Test
    void validatesFeedbackAndOnlyAcceptsRelativePagePaths() {
        try (var factory = Validation.buildDefaultValidatorFactory()) {
            var validator = factory.getValidator();
            assertThat(validator.validate(new FeedbackRequest("Góp ý", null))).isEmpty();
            assertThat(validator.validate(REQUEST)).isEmpty();
            assertThat(validator.validate(new FeedbackRequest(" ", "/search?private=1"))).hasSize(2);
            assertThat(validator.validate(new FeedbackRequest("x".repeat(2001), "//example.com"))).hasSize(2);
            assertThat(validator.validate(new FeedbackRequest("Góp ý", "/search\n"))).hasSize(1);
        }
    }
}
