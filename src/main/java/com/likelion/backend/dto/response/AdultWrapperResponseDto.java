package com.likelion.backend.dto.response;

import lombok.Builder;
import lombok.Getter;

import java.util.List;

@Getter
@Builder
public class AdultWrapperResponseDto {
    private int memberCount;
    private List<AdultResponseDto> results;

}
