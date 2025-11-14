package com.likelion.backend.dto.request;

import com.likelion.backend.domain.Member;
import lombok.Getter;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import java.util.Map;

@Getter
public class JoinRequestDto {
    private String name;
    private String password;
    private Map<String, String> answers;

    // 회원가입
    public Member toEntity(BCryptPasswordEncoder bCryptPasswordEncoder){
        return Member.builder()
                .name(this.name)
                .password(bCryptPasswordEncoder.encode(this.password))
                .build();
    }
}
