package com.likelion.backend.service;

import com.likelion.backend.dto.response.QuestionResponseDto;
import com.likelion.backend.repository.QuestionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class QuestionService {

    private final QuestionRepository questionRepository;

    public List<QuestionResponseDto> getQuestion(){
        return questionRepository.findAll().stream()
                .map(QuestionResponseDto::fromEntity)
                .toList();
    }
}
