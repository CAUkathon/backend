package com.likelion.backend.dto.response;

import com.likelion.backend.domain.Question;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
@AllArgsConstructor
public class QuestionResponseDto {

    private Long questionId;
    private String content;

    public static QuestionResponseDto fromEntity(Question question) {
        return QuestionResponseDto.builder()
                .questionId(question.getId())
                .content(question.getContent())
                .build();
    }
}
