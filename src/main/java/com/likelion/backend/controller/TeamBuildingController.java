package com.likelion.backend.controller;

import com.likelion.backend.dto.request.TeamBuildingRequestDto;
import com.likelion.backend.dto.response.TeamOutputDto;
import com.likelion.backend.service.TeamBuildingService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/team")
@RequiredArgsConstructor
public class TeamBuildingController {

    private final TeamBuildingService teamBuildingService;

    @PostMapping
    public List<TeamOutputDto> buildTeams(@RequestBody TeamBuildingRequestDto dto) {
        return teamBuildingService.buildTeams(dto.getTotalMembers(), dto.getTeamCount());
    }
}
