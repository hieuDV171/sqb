package com.frozenheart.backend.core.entity.user;

import com.fasterxml.jackson.annotation.JsonFormat;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class QuietHour {

    private boolean enabled;

    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "HH:mm[:ss]")
    private LocalTime startTime;

    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "HH:mm[:ss]")
    private LocalTime endTime;

    private String timezone;

}
