package com.likelion.backend.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
@AllArgsConstructor
public class TeamMemberDto {
    private String name;
    private String mbti;          // MBTI 전체 문자열
    private String hobby;         // "취미&관심사"
    private String favoriteFood;  // "최애음식"
    private String wildLionAnswer;// "vs 사자"
    private int drinkScore;       // 음주 점수
    private String image;         // 이미지 URL
    private boolean leader;       // 리더 여부
}