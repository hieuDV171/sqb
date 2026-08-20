package com.frozenheart.backend.modules.auth.service;

public interface EmailService {
    void sendOtpEmail(String toEmail, String otpCode);
}
