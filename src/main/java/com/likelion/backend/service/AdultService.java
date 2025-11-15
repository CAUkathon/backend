package com.likelion.backend.service;

import com.likelion.backend.domain.QuestionResult;
import com.likelion.backend.dto.response.AdultResponseDto;
import com.likelion.backend.repository.QuestionResultRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AdultService {

    private final QuestionResultRepository questionResultRepository;

    public List<AdultResponseDto> getResultsGroupedByMember(){
        List<QuestionResult> results = questionResultRepository.findAll();

        // 1) memberName 기준으로 그룹핑
        Map<Long, List<QuestionResult>> grouped = results.stream()
                .collect(Collectors.groupingBy(result -> result.getMember().getId()));

        // 2) DTO로 변환
        return grouped.entrySet().stream()
                .map(entry -> AdultResponseDto.builder()
                        .memberId(entry.getKey())
                        .memberName(entry.getValue().get(0).getMember().getName())
                        .answers(entry.getValue().stream()
                                .map(QuestionResult::getAnswer)
                                .toList()
                        )
                        .build()
                )
                .toList();
    }
}
