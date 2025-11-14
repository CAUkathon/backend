package com.likelion.backend.service;

import com.likelion.backend.domain.Member;
import com.likelion.backend.domain.Question;
import com.likelion.backend.domain.QuestionResult;
import com.likelion.backend.dto.request.JoinRequestDto;
import com.likelion.backend.dto.request.LoginRequestDto;
import com.likelion.backend.dto.response.JoinResponseDto;
import com.likelion.backend.dto.response.LoginResponseDto;
import com.likelion.backend.dto.response.MyResponseDto;
import com.likelion.backend.repository.MemberRepository;
import com.likelion.backend.repository.QuestionRepository;
import com.likelion.backend.repository.QuestionResultRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.Map;

@Service
@RequiredArgsConstructor
public class MemberService {

    private final MemberRepository memberRepository;
    private final QuestionRepository questionRepository;
    private final QuestionResultRepository questionResultRepository;

    // 비밀번호 인코더 DI (생성자 주입)
    private final BCryptPasswordEncoder bCryptPasswordEncoder;

    public LoginResponseDto login(LoginRequestDto dto) {

        // 1) 사용자 조회
        Member member = memberRepository.findByName(dto.getName())
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 회원입니다."));

        // 2) 비밀번호 검증
        if (!bCryptPasswordEncoder.matches(dto.getPassword(), member.getPassword())) {
            throw new IllegalArgumentException("비밀번호가 틀렸습니다.");
        }

        // 질문 응답 여부
        boolean hasQuestion = questionResultRepository.existsByMember(member);

        return new LoginResponseDto(
                member.getId(),
                member.getName(),
                member.getRole().name(),
                hasQuestion
        );
    }

    public JoinResponseDto join(JoinRequestDto dto){

        // 1. 이미 존재하는 사용자면 예외 던짐 (회원가입이므로)
        if (memberRepository.existsByName(dto.getName())) {
            throw new IllegalArgumentException("이미 존재하는 사용자입니다.");
        }

        // 2. 신규 회원 생성
        Member member = dto.toEntity(bCryptPasswordEncoder);
        memberRepository.save(member);

        // 3. 질문 답변 저장 + 캐릭터 판정 준비
        Map<String, String> answers = dto.getAnswers();

        for (Map.Entry<String, String> entry : answers.entrySet()) {
            Long questionNumber = Long.parseLong(entry.getKey()); // 받는거만 string하고 Long으로 변환
            String answerText = entry.getValue();

            Question question = questionRepository.findById(questionNumber)
                    .orElseThrow(() -> new RuntimeException(questionNumber + "번 질문이 없습니다."));

            QuestionResult result = QuestionResult.builder()
                    .member(member)
                    .question(question)
                    .answer(answerText)
                    .build();

            questionResultRepository.save(result);
        }

        String resultType = null;
        boolean determined = false;

        String s1 = answers.get("1");
        if (s1 != null) {
            try {
                int v1 = Integer.parseInt(s1.trim());
                if (v1 >= 8) {
                    resultType = "팔로워형 사자";
                    determined = true;
                }
            } catch (NumberFormatException ignored) {}
        }


        if (!determined) {
            String s2 = answers.get("2");
            if (s2 != null) {
                try {
                    int v2 = Integer.parseInt(s2.trim());
                    if (v2 >= 8) {
                        resultType = "헤롱헤롱 사자";
                        determined = true;
                    }
                } catch (NumberFormatException ignored) {}
            }
        }

        if (!determined && answers.containsKey("5") && answers.containsKey("3")) {
            String v5 = answers.get("5") == null ? "" : answers.get("5").trim();
            String v3 = answers.get("3") == null ? "" : answers.get("3").trim();

            if (v3.length() > 0) {
                char first = Character.toUpperCase(v3.charAt(0));
                if ("밖순이".equals(v5) && first == 'E') {
                    resultType = "식빵 굽는 사자";
                    determined = true;
                } else if ("밖순이".equals(v5) && first == 'I') {
                    resultType = "친구를 만나느라 샤샤샤자";
                    determined = true;
                } else if ("집순이".equals(v5) && first == 'E') {
                    resultType = "반전 매력 사자";
                    determined = true;
                } else if ("집순이".equals(v5) && first == 'I') {
                    resultType = "이불 속 사자";
                    determined = true;
                }
            }
        }

        // 4. Member 의 answer 필드에 저장
        member.setAnswer(resultType);
        memberRepository.save(member);

        // 5. 응답 반환
        return JoinResponseDto.fromEntity(member);
    }


    public MyResponseDto my(Long id){

        Member member = memberRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 사용자입니다."));
        return MyResponseDto.fromEntity(member);
    }
}
