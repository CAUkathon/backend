package com.likelion.backend.dto.response;
import com.likelion.backend.domain.Member;
import com.likelion.backend.enums.Role;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
@AllArgsConstructor
public class JoinResponseDto {
    private Long id;
    private String name;
    private Role role;
    private String gender;
    private String answer;

    // DTO 변환
    public static JoinResponseDto fromEntity(Member member){

        // 질문 결과 list 관련

        return JoinResponseDto.builder()
                .id(member.getId())
                .name(member.getName())
                .role(member.getRole())
                .gender(member.getGender())
                .answer(member.getAnswer())
                .build();
    }
}
