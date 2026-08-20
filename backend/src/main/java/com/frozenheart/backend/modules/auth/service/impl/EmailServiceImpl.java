package com.frozenheart.backend.modules.auth.service.impl;

import java.nio.charset.StandardCharsets;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import com.frozenheart.backend.modules.auth.service.EmailService;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
@RequiredArgsConstructor
public class EmailServiceImpl implements EmailService {
    private final JavaMailSender mailSender;

    @Value("${spring.mail.username}")
    private String fromEmail;

    @Async

    public void sendOtpEmail(String toEmail, String otpCode) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(
                    message,
                    MimeMessageHelper.MULTIPART_MODE_MIXED_RELATED,
                    StandardCharsets.UTF_8.name());

            helper.setFrom(fromEmail);
            helper.setTo(toEmail);
            helper.setSubject("[SQB HUST] Mã xác thực đăng ký tài khoản");

            String htmlContent = """
                    <div style="font-family: Arial, sans-serif; max-width: 600px; margin: 0 auto; padding: 20px; border: 1px solid #e0e0e0; border-radius: 10px;">
                        <h2 style="color: #b22222; text-align: center;">MẠNG XÃ HỘI ĐẠI HỌC - SQB</h2>
                        <p>Xin chào,</p>
                        <p>Mã xác thực (OTP) để hoàn tất đăng ký tài khoản của bạn là:</p>
                        <div style="text-align: center; margin: 25px 0;">
                            <span style="font-size: 32px; font-weight: bold; letter-spacing: 5px; color: #b22222; background-color: #f8f9fa; padding: 10px 20px; border-radius: 6px; border: 1px dashed #b22222;">%s</span>
                        </div>
                        <p style="color: #555;">Mã này có hiệu lực trong <b>2 phút</b>. Vui lòng không chia sẻ mã này với bất kỳ ai.</p>
                        <hr style="border: none; border-top: 1px solid #eee; margin: 20px 0;" />
                        <p style="font-size: 12px; color: #888; text-align: center;">Đây là email tự động, vui lòng không phản hồi email này.</p>
                    </div>
                    """
                    .formatted(otpCode);

            helper.setText(htmlContent, true);

            mailSender.send(message);
            log.info("Đã gửi email OTP thành công tới: {}", toEmail);
        } catch (MessagingException e) {
            log.error("Lỗi khi gửi email OTP tới {}: {}", toEmail, e.getMessage());
        }
    }

}
