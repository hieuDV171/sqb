package com.frozenheart.backend.modules.user.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.frozenheart.backend.core.dto.GlobalResponse;
import com.frozenheart.backend.modules.user.dto.ProfileResponse;
import com.frozenheart.backend.modules.user.dto.UpdateProfileRequest;
import com.frozenheart.backend.modules.user.service.UserService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    @GetMapping("/me")
    public ResponseEntity<GlobalResponse<ProfileResponse>> getMyProfile() {
        ProfileResponse response = userService.getMyProfile();

        return ResponseEntity.ok(GlobalResponse.success(response));
    }

    @GetMapping("/{userId}")
    public ResponseEntity<GlobalResponse<ProfileResponse>> getUserProfile(@PathVariable Long userId) {
        ProfileResponse response = userService.getUserProfile(userId);

        return ResponseEntity.ok(GlobalResponse.success(response));
    }

    @PutMapping("/me")
    public ResponseEntity<GlobalResponse<ProfileResponse>> updateMyProfile(
            @Valid @RequestBody UpdateProfileRequest request) {
        ProfileResponse response = userService.updateMyProfile(request);

        return ResponseEntity.ok(GlobalResponse.success(response));
    }
    
    @DeleteMapping("/me")
    public ResponseEntity<GlobalResponse<Void>> deleteMyProfile() {
        userService.deleteMyProfile();

        return ResponseEntity.ok(GlobalResponse.success());
    }

}
