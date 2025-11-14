package com.likelion.backend.dto.request;

import com.likelion.backend.domain.Question;
import com.likelion.backend.enums.QuestionType;
import jakarta.persistence.ElementCollection;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.util.List;

@Getter
@Builder
@AllArgsConstructor
public class QuestionRequestDto {
    private String content; // 질문 제목
    private String inputType; // 클라리언특 보내는 값
    private QuestionType type;
    private List<String> choices;

    public QuestionType toQuestionType() {
        return switch (inputType.toUpperCase()) {
            case "METRIC" -> QuestionType.METRIC;
            case "STRING" -> QuestionType.STRING;
            case "CHOICES" -> QuestionType.CHOICES;
            default -> throw new IllegalArgumentException("잘못된 InputType입니다.");
        };
    }

    public Question toEntity() {
        return Question.builder()
                .content(this.content)
                .type(toQuestionType())
                .choices(this.choices)
                .build();
    }
}