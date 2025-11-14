package com.likelion.backend.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
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
@JsonInclude(JsonInclude.Include.NON_NULL)
public class QuestionResponseDto {

    private Long questionId;
    private String content;
    //private QuestionType type;
    private List<String> choices;

    public static QuestionResponseDto fromEntity(Question question) {
        List<String> choicesToReturn = null;

        if (question.getType() == QuestionType.CHOICES) {
            choicesToReturn = question.getChoices();
        }
        else if(question.getType() == QuestionType.METRIC){
            choicesToReturn = question.getChoices();
        }

        return QuestionResponseDto.builder()
                .questionId(question.getId())
                .content(question.getContent())
                .choices(choicesToReturn)
                .build();
    }
}
