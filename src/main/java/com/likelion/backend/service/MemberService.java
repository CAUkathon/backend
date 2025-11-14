package com.likelion.backend.service;

import com.likelion.backend.domain.Member;
import com.likelion.backend.dto.request.JoinRequestDto;
import com.likelion.backend.dto.response.JoinResponseDto;
import com.likelion.backend.dto.response.MyResponseDto;
import com.likelion.backend.repository.MemberRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class MemberService {

    private final MemberRepository memberRepository;
    // 비밀번호 인코더 DI (생성자 주입)
    private final BCryptPasswordEncoder bCryptPasswordEncoder;

    public JoinResponseDto login(JoinRequestDto joinRequestDto) {
        // 해당 name이 이미 존재시
        if (memberRepository.existsByName(joinRequestDto.getName())){
            Member member = memberRepository.findByName(joinRequestDto.getName())
                    .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 사용자입니다."));
            // 비번 검사
            if (!bCryptPasswordEncoder.matches(joinRequestDto.getPassword(),member.getPassword())){
                throw new IllegalArgumentException("비밀번호가 올바르지 않습니다.");
            }
            return JoinResponseDto.fromEntity(member);
        }

        // 유저 객체 생성
        Member member = joinRequestDto.toEntity(bCryptPasswordEncoder);

        memberRepository.save(member);

        return JoinResponseDto.fromEntity(member);
    }

    public MyResponseDto my(Long id){

        Member member = memberRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 사용자입니다."));
        return MyResponseDto.fromEntity(member);
    }
}
