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
        String description = "";
        String type = member.getAnswer();
        if (type.equals("나폴레옹 사자")){
            description = "카리스마 뿜뿜! 리더십과 결단력이 강해서 어디서든 중심이 되는 사자.";
        }
        else if (type.equals("헤롱헤롱 사자")){
            description = "즐거움을 사랑하는 파티형 사자. 내가 바로 술자리 분위기 메이커!";
        }
        else if (type.equals("이불 속 사자")){
            description = "이불 밖은 위험해 ㅠㅠ 오늘 만큼은 밀림의 왕이 아닌 집콕의 왕!";
        }
        else if (type.equals("식빵 굽는 사자")){
            description = "밖으로 나가면 에너지 폭발! 사람들과 어울리며 새로운 경험을 즐기는 사자.";
        }
        else if (type.equals("친구를 만나느라 샤샤샤자")){
            description = "외출은 귀찮지만, 친한 친구만 있으면 OK! 찾았다, 멋사의 소모임을 이끌어 갈 사자!";
        }
        else if (type.equals("반전 매력 사자")){
            description = "밖에서는 쾌활, 집에서는 느긋… 두 얼굴의 매력둥이 사자. 알고 보면 은근 귀여움 폭발!";
        }
        return MyResponseDto.fromEntity(member, description);
    }
}
