package com.likelion.backend.controller;

import com.likelion.backend.dto.request.TeamBuildingRequestDto;
import com.likelion.backend.dto.response.TeamBuildingResponseDto;
import com.likelion.backend.service.TeamBuildingService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/team")
@RequiredArgsConstructor
public class TeamBuildingController {

    private final TeamBuildingService teamBuildingService;

    @PostMapping
    public TeamBuildingResponseDto buildTeams(@RequestBody TeamBuildingRequestDto dto) {
        return teamBuildingService.buildTeams(dto.getTotalMembers(), dto.getTeamCount());
    }
}
