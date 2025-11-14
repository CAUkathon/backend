package com.likelion.backend.enums;

import lombok.Getter;

@Getter
public enum QuestionType {
    METRIC, // 1~10 점의 척도
    STRING, // 문장 단순 입력
    CHOICES; // string형 문장 선택
}
