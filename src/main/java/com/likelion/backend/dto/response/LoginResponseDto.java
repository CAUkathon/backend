package com.likelion.backend.dto.response;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class LoginResponseDto {
    // private String accessToken;
    private Long memberId;
    private String name;
    private String role;
    private boolean hasQuestionResult;
}
