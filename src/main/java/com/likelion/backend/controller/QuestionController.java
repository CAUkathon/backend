package com.likelion.backend.controller;

import com.likelion.backend.domain.Question;
import com.likelion.backend.dto.request.QuestionRequestDto;
import com.likelion.backend.dto.response.QuestionResponseDto;
import com.likelion.backend.service.QuestionService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/question")
public class QuestionController {
    private final QuestionService questionService;

    @PostMapping
    public ResponseEntity<QuestionResponseDto> postQuestion(@RequestBody QuestionRequestDto questionRequestDto){
        return ResponseEntity.ok(questionService.postQuestion(questionRequestDto));
    }

    @GetMapping()
    public ResponseEntity<List<QuestionResponseDto>> getAllQuestions() {
        return ResponseEntity.ok(questionService.getQuestion());
    }
}
