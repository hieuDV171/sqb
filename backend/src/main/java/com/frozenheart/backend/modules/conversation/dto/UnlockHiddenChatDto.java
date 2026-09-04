package com.frozenheart.backend.modules.conversation.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.*;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UnlockHiddenChatDto {

    @NotBlank(message = "Mã PIN không được để trống")
    private String pin;
}
