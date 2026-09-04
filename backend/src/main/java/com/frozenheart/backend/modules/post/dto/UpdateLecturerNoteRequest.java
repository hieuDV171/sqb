package com.frozenheart.backend.modules.post.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UpdateLecturerNoteRequest {
    @NotBlank(message = "Nội dung không thể trống")
    private String content;
}
