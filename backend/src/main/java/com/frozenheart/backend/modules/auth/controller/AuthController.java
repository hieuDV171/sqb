package com.frozenheart.backend.modules.auth.controller;

import com.frozenheart.backend.modules.auth.dto.admin.AuthResponse;
import com.frozenheart.backend.modules.auth.dto.user.ChangePasswordRequest;
import com.frozenheart.backend.modules.auth.dto.user.LoginRequest;
import com.frozenheart.backend.modules.auth.dto.user.RefreshTokenRequest;
import com.frozenheart.backend.modules.auth.dto.user.RefreshTokenResponse;
import com.frozenheart.backend.modules.auth.service.AuthService;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.frozenheart.backend.core.dto.GlobalResponse;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @PostMapping("/login")
    public ResponseEntity<GlobalResponse<AuthResponse>> login(@Valid @RequestBody LoginRequest request) {
        AuthResponse response = authService.login(request);

        return ResponseEntity.ok(GlobalResponse.success(response));
    }

    @PostMapping("/logout")
    public ResponseEntity<GlobalResponse<Void>> logout() {

        authService.logout();

        return ResponseEntity.ok(GlobalResponse.success());
    }

    @PostMapping("/change-password")
    public ResponseEntity<GlobalResponse<Void>> changePassword(@Valid @RequestBody ChangePasswordRequest request) {
        authService.changePassword(request);
        return ResponseEntity.ok(GlobalResponse.success());
    }

    @PostMapping("/refresh")
    public ResponseEntity<GlobalResponse<RefreshTokenResponse>> refreshToken(
            @Valid @RequestBody(required = false) RefreshTokenRequest request
    ) {
        RefreshTokenResponse response = authService.refreshToken(request);
        return ResponseEntity.ok(GlobalResponse.success(response));
    }

}
