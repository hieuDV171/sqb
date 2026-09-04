package com.frozenheart.backend.modules.socialinteraction.controller;

import com.frozenheart.backend.core.dto.GlobalResponse;
import com.frozenheart.backend.modules.socialinteraction.dto.ReactRequest;
import com.frozenheart.backend.modules.socialinteraction.dto.ReactResponseDto;
import com.frozenheart.backend.modules.socialinteraction.service.ReactService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class ReactController {

    private final ReactService reactService;

    @PostMapping("/react")
    public ResponseEntity<GlobalResponse<ReactResponseDto>> toggleReact(
            @Valid @RequestBody ReactRequest request) {
        ReactResponseDto response = reactService.toggleReact(request);
        return ResponseEntity.ok(GlobalResponse.success(response));
    }
}
