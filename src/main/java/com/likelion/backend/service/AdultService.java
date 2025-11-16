package com.likelion.backend.service;

import com.likelion.backend.domain.QuestionResult;
import com.likelion.backend.dto.response.AdultResponseDto;
import com.likelion.backend.dto.response.AdultWrapperResponseDto;
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

    public AdultWrapperResponseDto getResultsGroupedByMember(){
        List<QuestionResult> results = questionResultRepository.findAll();

        // 1) memberName 기준으로 그룹핑
        Map<Long, List<QuestionResult>> grouped = results.stream()
                .collect(Collectors.groupingBy(result -> result.getMember().getId()));

        // 2) DTO로 변환
        List<AdultResponseDto> dtoList = grouped.entrySet().stream()
                .map(entry -> {
                            Long memberId = entry.getKey();
                            List<QuestionResult> qrList = entry.getValue();

                            // answers를 Map<questionKeyword, answer> 형태로 변환
                            Map<String, String> answersMap = qrList.stream()
                                    .collect(Collectors.toMap(
                                            qr -> qr.getQuestion().getKeyword(), // key
                                            QuestionResult::getAnswer                    // value
                                    ));

                            return AdultResponseDto.builder()
                                    .memberId(memberId)
                                    .memberName(qrList.get(0).getMember().getName())
                                    .answers(answersMap)
                                    .build();
                        }
                )
                .toList();

        return AdultWrapperResponseDto.builder()
                .memberCount(dtoList.size())
                .results(dtoList)
                .build();

    }
}
