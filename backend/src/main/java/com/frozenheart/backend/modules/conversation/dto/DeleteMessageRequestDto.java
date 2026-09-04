package com.frozenheart.backend.modules.conversation.dto;

import com.frozenheart.backend.modules.conversation.constant.MessageDeleteScope;
import jakarta.validation.constraints.NotNull;
import lombok.*;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DeleteMessageRequestDto {

    @NotNull(message = "Scope không được để trống (ME hoặc EVERYONE)")
    private MessageDeleteScope scope;
}
