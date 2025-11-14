package com.likelion.backend.dto.request;

import lombok.Getter;

@Getter
public class LoginRequestDto {
    private String name;
    private String password;
}