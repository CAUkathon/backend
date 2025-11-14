package com.likelion.backend.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
@AllArgsConstructor
public class TeamMemberDto {
    private String name;
    private String mbti;
    private int drinkScore;
    private boolean leader;
}
