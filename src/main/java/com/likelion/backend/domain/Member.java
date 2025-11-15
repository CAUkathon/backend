package com.likelion.backend.domain;

import com.likelion.backend.enums.Role;
import jakarta.persistence.*;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import static com.likelion.backend.enums.Role.BABY;

@Entity
@Getter
@NoArgsConstructor
public class Member extends BaseTimeEntity{
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String name;
    private String password;
    // 결과 내용 Field 속성도 추가할 것
    @Enumerated(EnumType.STRING)
    private Role role;

    private String gender;
    private String answer;

    // 생성자
    @Builder
    public Member(String name, String password, Role role, String gender) {
        this.name = name;
        this.password = password;
        this.role = role == null ? BABY : role;
        this.gender = gender;
    }

    public boolean isBaby(){return Role.BABY.equals(this.role);}
    public boolean isAdult(){return Role.ADULT.equals(this.role);}

    public void setAnswer(String answer) {
        this.answer = answer;
    }
}
