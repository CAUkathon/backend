package com.likelion.backend.domain;

import com.likelion.backend.enums.QuestionType;
import jakarta.persistence.*;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

@Entity
@Getter
@NoArgsConstructor
public class Question extends BaseTimeEntity{

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String content; // 질문 이름

    private QuestionType type; // 질문 유형 (척도, 주관식입력, 선택형)

    private String keyword; //질문 키워드

    @ElementCollection
    @CollectionTable(name = "question_choices", joinColumns = @JoinColumn(name = "question_id"))
    @Column(name = "choice")
    private List<String> choices; // 선택형 질문 보기들.

    // 생성자
    @Builder
    public Question(String content, QuestionType type, List<String> choices){
        this.content = content;
        this.type = type;
        this.choices = new ArrayList<>(choices);
        this.keyword = "";
    }

}
