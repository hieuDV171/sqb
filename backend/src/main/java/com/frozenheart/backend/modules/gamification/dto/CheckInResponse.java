package com.frozenheart.backend.modules.gamification.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CheckInResponse {

    private double coinEarned;
    private double currentCoinBalance;
    private int currentStreak;
    private LocalDate checkInDate;
    private String message;

}
