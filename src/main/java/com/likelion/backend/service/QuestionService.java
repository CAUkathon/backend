package com.likelion.backend.service;

import com.likelion.backend.domain.Question;
import com.likelion.backend.dto.request.QuestionRequestDto;
import com.likelion.backend.dto.response.QuestionResponseDto;
import com.likelion.backend.repository.QuestionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class QuestionService {

    private final QuestionRepository questionRepository;

    public QuestionResponseDto postQuestion(QuestionRequestDto questionRequestDto){
        // 질문 생성
        Question question = questionRequestDto.toEntity();
        questionRepository.save(question);
        return QuestionResponseDto.fromEntity(question);
    }

    public List<QuestionResponseDto> getQuestion(){
        return questionRepository.findAll().stream()
                .map(QuestionResponseDto::fromEntity)
                .toList();
    }
}
