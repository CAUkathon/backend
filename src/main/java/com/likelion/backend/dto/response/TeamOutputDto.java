package com.likelion.backend.dto.response;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.List;

@Getter
@AllArgsConstructor
public class TeamOutputDto {
    private String teamName;
    private List<TeamMemberDto> members;
}
