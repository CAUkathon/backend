package com.likelion.backend.controller;

import com.likelion.backend.dto.request.TeamBuildingRequestDto;
import com.likelion.backend.dto.response.TeamOutputDto;
import com.likelion.backend.repository.MemberRepository;
import com.likelion.backend.service.TeamBuildingService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/team")
@RequiredArgsConstructor
public class TeamBuildingController {

    private final TeamBuildingService teamBuildingService;
    private final MemberRepository memberRepository;


    @PostMapping
    public ResponseEntity<?> buildTeams(@RequestBody TeamBuildingRequestDto dto) {
        try {
            List<TeamOutputDto> teams = teamBuildingService.buildAndSaveTeams(dto.getTotalMembers(), dto.getTeamCount());
            return ResponseEntity.ok(teams);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @GetMapping
    public ResponseEntity<?> getTeamBuildingResults() {
        List<TeamOutputDto> teams = teamBuildingService.getAllTeams();
        boolean hasUnbuiltMembers = memberRepository.findAll().stream()
                .anyMatch(m -> !m.isTeamBuilt());

        Map<String, Object> response = new HashMap<>();
        response.put("teams", teams);
        response.put("hasUnbuiltMembers", hasUnbuiltMembers);

        return ResponseEntity.ok(response);
    }

    @DeleteMapping
    public ResponseEntity<String> deleteTeamBuildingResults() {
        try {
            teamBuildingService.clearAllTeams();
            return ResponseEntity.ok("모든 팀 데이터가 삭제되었습니다.");
        } catch (Exception e) {
            return ResponseEntity.status(500).body("삭제 중 오류 발생");
        }
    }
}
