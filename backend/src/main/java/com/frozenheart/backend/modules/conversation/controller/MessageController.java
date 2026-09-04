package com.frozenheart.backend.modules.conversation.controller;

import com.frozenheart.backend.core.dto.GlobalResponse;
import com.frozenheart.backend.modules.conversation.dto.DeleteMessageRequestDto;
import com.frozenheart.backend.modules.conversation.dto.DeleteMessageResponseDto;
import com.frozenheart.backend.modules.conversation.dto.MessageListResponseDto;
import com.frozenheart.backend.modules.conversation.service.MessageService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping
@RequiredArgsConstructor
public class MessageController {

    private final MessageService messageService;

    @GetMapping("/conversations/{id}/messages")
    public ResponseEntity<GlobalResponse<MessageListResponseDto>> getMessages(
            @PathVariable("id") Long conversationId,
            @RequestParam(value = "after", required = false) Long after,
            @RequestParam(value = "limit", required = false, defaultValue = "20") Integer limit) {
        MessageListResponseDto response = messageService.getMessages(conversationId, after, limit);
        return ResponseEntity.ok(GlobalResponse.success(response));
    }

    @DeleteMapping("/messages/{id}")
    public ResponseEntity<GlobalResponse<DeleteMessageResponseDto>> deleteMessage(
            @PathVariable("id") Long messageId,
            @Valid @RequestBody DeleteMessageRequestDto request) {
        DeleteMessageResponseDto response = messageService.deleteMessage(messageId, request);
        return ResponseEntity.ok(GlobalResponse.success(response));
    }
}
