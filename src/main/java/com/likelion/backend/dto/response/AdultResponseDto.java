package com.likelion.backend.dto.response;

import com.likelion.backend.domain.QuestionResult;
import lombok.Builder;
import lombok.Getter;

import java.util.List;
import java.util.Map;

@Getter
@Builder
public class AdultResponseDto {
    private Long memberId;
    private String memberName;
    private Map<String,String> answers;

}
