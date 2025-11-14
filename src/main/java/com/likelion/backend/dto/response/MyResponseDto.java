package com.likelion.backend.dto.response;

import com.likelion.backend.domain.Member;
import com.likelion.backend.enums.Role;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
@AllArgsConstructor
public class MyResponseDto {
    private Long id;
    private String name;
    private Role role;
    // 질문 관련 내용 private List ..
    // 이미지 관련 내용

    // DTO 변환
    public static MyResponseDto fromEntity(Member member){

        // 질문 결과 list 관련

        return MyResponseDto.builder()
                .id(member.getId())
                .name(member.getName())
                .role(member.getRole())
                .build();
    }
}
