package com.likelion.backend.controller;

import com.likelion.backend.dto.response.AdultResponseDto;
import com.likelion.backend.service.AdultService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/adult")
public class AdultController {
    private final AdultService adultService;

    @GetMapping
    public List<AdultResponseDto> getGroupedResults(){
        return adultService.getResultsGroupedByMember();
    }
}
