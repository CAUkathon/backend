package com.likelion.backend.dto.response;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.List;

@Getter
@AllArgsConstructor
public class TeamBuildingResponseDto {
    private List<List<Long>> teams;
}
