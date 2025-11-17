package com.likelion.backend.service;

import com.likelion.backend.domain.Member;
import com.likelion.backend.domain.QuestionResult;
import com.likelion.backend.domain.Team;
import com.likelion.backend.dto.response.TeamMemberDto;
import com.likelion.backend.dto.response.TeamOutputDto;
import com.likelion.backend.enums.Role;
import com.likelion.backend.repository.MemberRepository;
import com.likelion.backend.repository.QuestionResultRepository;
import com.likelion.backend.repository.TeamRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class TeamBuildingService {

    private final MemberRepository memberRepository;
    private final QuestionResultRepository questionResultRepository;
    private final TeamRepository teamRepository;

    // 총 멤버 수, 만들 팀 수 입력받음
    public List<TeamOutputDto> buildBalancedTeams(int totalMembers, int teamCount) {

        // 아기사자 대상으로 한정
        List<Member> allMembers = memberRepository.findByRole(Role.BABY).stream()
                .sorted(Comparator.comparing(Member::getId))  // ID 오름차순 정렬해서 오래된 멤버부터
                .toList();
        List<Member> members = allMembers.subList(0, Math.min(totalMembers, allMembers.size()));

        // 1. 리더 점수로 리더 선발 <- 질문 1 답이 높을수록 리더형!
        Map<Member, Integer> leaderScoreMap = new HashMap<>();
        for (Member m : members) {
            leaderScoreMap.put(m, getLeaderScore(m));
        }

        // 리더 점수 높은 순으로 정렬하여 팀 수만큼 리더 선발
        List<Member> leaders = leaderScoreMap.entrySet().stream()
                .sorted(Map.Entry.<Member, Integer>comparingByValue(Comparator.reverseOrder())
                        .thenComparing(e -> e.getKey().getId()))
                .limit(teamCount)
                .map(Map.Entry::getKey)
                .collect(Collectors.toList());


        // 2. 리더 음주 점수 추출 & 리더 정렬 (음주 내림차순) <- 질문 2 답이 높을수록 애주가
        leaders.sort((a, b) -> Integer.compare(getDrinkScore(b), getDrinkScore(a)));

        // 3. 리더 제외 팔로워 분리
        List<Member> followers = new ArrayList<>(members);
        followers.removeAll(leaders);

        // 4. 팔로워를 MBTI E/I 분류 및 음주 점수 내림차순 정렬
        Map<Boolean, List<Member>> grouped = followers.stream()
                .collect(Collectors.groupingBy(m -> "E".equalsIgnoreCase(getMbtiFirstLetter(m))));
        List<Member> eGroup = grouped.getOrDefault(true, new ArrayList<>());
        List<Member> iGroup = grouped.getOrDefault(false, new ArrayList<>());

        Comparator<Member> drinkDescThenId = Comparator.comparingInt(this::getDrinkScore).reversed()
                .thenComparing(Member::getId);

        eGroup.sort(drinkDescThenId);
        iGroup.sort(drinkDescThenId);

        // 성별로 다시 분리 및 음주 점수 내림차순 정렬 (남자/여자 각각)
        List<Member> eMaleGroup = eGroup.stream()
                .filter(m -> "남자".equalsIgnoreCase(m.getGender()))
                .collect(Collectors.toList());
        List<Member> eFemaleGroup = eGroup.stream()
                .filter(m -> "여자".equalsIgnoreCase(m.getGender()))
                .collect(Collectors.toList());
        List<Member> iMaleGroup = iGroup.stream()
                .filter(m -> "남자".equalsIgnoreCase(m.getGender()))
                .collect(Collectors.toList());
        List<Member> iFemaleGroup = iGroup.stream()
                .filter(m -> "여자".equalsIgnoreCase(m.getGender()))
                .collect(Collectors.toList());

        eMaleGroup.sort(drinkDescThenId);
        eFemaleGroup.sort(drinkDescThenId);
        iMaleGroup.sort(drinkDescThenId);
        iFemaleGroup.sort(drinkDescThenId);

        // 5. 팀별 균등 인원 할당 수 계산 (리더 1명 제외)
        int baseCount = totalMembers / teamCount;
        int remainder = totalMembers % teamCount;

        List<List<TeamMemberDto>> teamsMembers = new ArrayList<>();
        for (int i = 0; i < teamCount; i++) {
            teamsMembers.add(new ArrayList<>());
        }

        // 6. 리더를 팀별 첫 번째 멤버로 배정
        for (int i = 0; i < teamCount; i++) {
            Member leader = leaders.get(i);
            teamsMembers.get(i).add(toDto(leader, true));
        }

        // 7. 팔로워들을 남은 자리에 최대한 균등 분배 - 라온드로빈 방식 (MBTI, 성별, 음주 점수 고려했음)
        List<Member> remain = new ArrayList<>();
        remain.addAll(eMaleGroup);
        remain.addAll(eFemaleGroup);
        remain.addAll(iMaleGroup);
        remain.addAll(iFemaleGroup);

        int[] slot = new int[teamCount];
        for (int i = 0; i < teamCount; i++) {
            // 리더 자리 제외하고 배정 가능한 슬롯 계산
            slot[i] = baseCount + (i < remainder ? 1 : 0) - 1;
        }

        // 라운드로빈 방식으로 각 팀에 가장 적합한 멤버를 찾으며 채움
        while (!remain.isEmpty()) {
            Member bestMember = null;
            int bestTeamIdx = -1;
            double bestScore = Double.MAX_VALUE;

            for (Member m : remain) {
                int drinkScore = getDrinkScore(m);

                for (int t = 0; t < teamCount; t++) {
                    if (slot[t] <= 0) continue;

                    List<TeamMemberDto> tmp = new ArrayList<>(teamsMembers.get(t));
                    tmp.add(toDto(m, false));

                    // MBTI, 성별 균형 계산
                    int eCount = (int) tmp.stream().filter(mem -> "E".equalsIgnoreCase(mem.getMbti())).count();
                    long maleCount = tmp.stream().filter(mem -> "남자".equalsIgnoreCase(
                            memberRepository.findByName(mem.getName()).map(Member::getGender).orElse(""))).count();

                    double avgDrink = tmp.stream().mapToInt(TeamMemberDto::getDrinkScore).average().orElse(0);
                    double drinkGap = Math.abs(avgDrink - drinkScore);

                    // 균형 점수 계산 (0.1 가중치로 음주 점수 편차 반영)
                    double score = Math.abs((double) eCount / tmp.size() - 0.5)
                            + Math.abs((double) maleCount / tmp.size() - 0.5)
                            + drinkGap * 0.1;

                    if (score < bestScore) {
                        bestScore = score;
                        bestMember = m;
                        bestTeamIdx = t;
                    }
                }
            }

            if (bestMember != null && bestTeamIdx != -1) {
                teamsMembers.get(bestTeamIdx).add(toDto(bestMember, false));
                slot[bestTeamIdx]--;
                remain.remove(bestMember);
            } else {
                // 적합한 멤버-팀 쌍이 더 없으면 중단
                break;
            }
        }

        // 8. 남은 멤버 처리 - 음주 점수 내림차순으로 정렬
        // 팀별 현재 음주점수 평균 계산 후 균형 맞춰 순차 배정
        double[] teamDrinkAverages = new double[teamCount];
        for (int i = 0; i < teamCount; i++) {
            List<TeamMemberDto> teamList = teamsMembers.get(i);
            double sum = teamList.stream().mapToInt(TeamMemberDto::getDrinkScore).sum();
            teamDrinkAverages[i] = teamList.isEmpty() ? 0 : sum / teamList.size();
        }

        List<Member> remainingMembers = new ArrayList<>(remain);
        remainingMembers.sort(drinkDescThenId);

        // 팀별 성별 인원 수 계산 함수
        Function<List<TeamMemberDto>, Map<String, Long>> genderCountInTeam = teamList -> teamList.stream()
                .collect(Collectors.groupingBy(tm -> memberRepository.findByName(tm.getName())
                        .map(Member::getGender)
                        .orElse("unknown"), Collectors.counting()));

        // 남은 멤버를 음주 점수가 가장 근접한 팀에 순차 배정
        for (Member m : remainingMembers) {
            int drinkScore = getDrinkScore(m);
            String gender = m.getGender();
            //음주점수 거리가 최소인 팀 선택
            int minIndex = 0;
            double minDiff = Double.MAX_VALUE;

            for (int i = 0; i < teamCount; i++) {
                List<TeamMemberDto> team = teamsMembers.get(i);
                double diff = Math.abs(teamDrinkAverages[i] - drinkScore);

                Map<String, Long> genderCount = genderCountInTeam.apply(team);
                long genderNum = genderCount.getOrDefault(gender, 0L);

                // 성별 균형을 고려한 페널티 부여
                long maxGenderCount = genderCount.values().stream().max(Long::compare).orElse(0L);
                if (genderNum == maxGenderCount && maxGenderCount > 0) {
                    diff *= 1.5;
                }

                if (diff < minDiff) {
                    minDiff = diff;
                    minIndex = i;
                }
            }
            teamsMembers.get(minIndex).add(toDto(m, false));

            // 팀 음주평균 업데이트 (간단 평균 재계산한거임)
            List<TeamMemberDto> updatedTeam = teamsMembers.get(minIndex);
            double sum = updatedTeam.stream().mapToDouble(TeamMemberDto::getDrinkScore).sum();
            teamDrinkAverages[minIndex] = sum / updatedTeam.size();
        }
        // 9. 최종 팀 Output DTO 생성 - 팀 이름과 각 팀 멤버 리스트로 결과 DTO 생성 및 반환
        List<TeamOutputDto> result = new ArrayList<>();
        for (int i = 0; i < teamCount; i++) {
            result.add(new TeamOutputDto("Team " + (i + 1), teamsMembers.get(i)));
        }
        return result;
    }

    // Member -> TeamMemberDto 변환 헬퍼 메서드
    private TeamMemberDto toDto(Member m, boolean leader) {
        return TeamMemberDto.builder()
                .name(m.getName())
                .mbti(getMbtiFirstLetter(m))
                .drinkScore(getDrinkScore(m))
                .leader(leader)
                .build();
    }

    // 멤버별 리더 점수 계산 (질문 내용 "대화" 포함)
    private int getLeaderScore(Member member) {
        return questionResultRepository.findByMember(member).stream()
                .filter(qr -> qr.getQuestion() != null && qr.getQuestion().getContent().contains("대화"))
                .map(qr -> {
                    try {
                        return Integer.parseInt(qr.getAnswer());
                    } catch (Exception e) {
                        return 0;  // 파싱 실패 시 0점으로 처리
                    }
                })
                .findFirst()
                .orElse(0);  // 질문 답변 없으면 0점 처리
    }


    // 멤버별 MBTI 첫 글자 반환
    private String getMbtiFirstLetter(Member member) {
        return questionResultRepository.findByMember(member).stream()
                .filter(qr -> qr.getQuestion()!=null && qr.getQuestion().getContent().contains("MBTI"))
                .map(qr -> {
                    String ans = qr.getAnswer();
                    return (ans!=null && !ans.isEmpty()) ? ans.substring(0,1).toUpperCase() : "";
                }).findFirst().orElse("");
    }

    // 멤버별 음주 점수(회식 관련 질문) 반환
    private int getDrinkScore(Member member) {
        return questionResultRepository.findByMember(member).stream()
                .filter(qr -> qr.getQuestion()!=null && qr.getQuestion().getContent().contains("회식"))
                .map(qr -> {
                    try { return Integer.parseInt(qr.getAnswer()); }
                    catch(Exception e){return 0;}
                }).findFirst().orElse(0);
    }

    // 멤버별 전체 MBTI 값 반환
    private String getMbtiFull(Member member) {
        return questionResultRepository.findByMember(member).stream()
                .filter(qr -> qr.getQuestion() != null
                        && qr.getQuestion().getContent().contains("MBTI"))
                .map(QuestionResult::getAnswer)
                .findFirst()
                .orElse("");
    }

    // 특정 질문코드로 답변 반환
    private String getAnswerByQuestionContent(Member member, String questionCode) {
        return questionResultRepository.findByMember(member).stream()
                .filter(qr -> qr.getQuestion() != null
                        && (qr.getQuestion().getContent() != null && qr.getQuestion().getContent().contains(questionCode)))
                .map(QuestionResult::getAnswer)
                .findFirst()
                .orElse("");
    }

    //팀을 빌딩하고 결과를 DB에 저장하는 메서드
    @Transactional
    public List<TeamOutputDto> buildAndSaveTeams(int totalMembers, int teamCount) {

        int currentMemberCount = (int) memberRepository.countByRole(Role.BABY);
        if (totalMembers > currentMemberCount) {
            throw new IllegalArgumentException("입력하신 회원 수가 존재하는 회원 수보다 많습니다." + " 존재하는 회원 수: " + currentMemberCount);
        }
        else if (totalMembers < currentMemberCount) {
            throw new IllegalArgumentException("존재하는 회원 수가 입력하신 회원 수보다 많습니다." + " 존재하는 회원 수: " + currentMemberCount);
        }
        if (teamCount > totalMembers) {
            throw new IllegalArgumentException("팀 개수가 전체 멤버 수보다 많을 수 없습니다.");
        }

        List<TeamOutputDto> teams = buildBalancedTeams(totalMembers, teamCount);

        // 기존 팀 데이터 삭제
        teamRepository.deleteAll();

        // 멤버 전체 teamBuilt 필드를 true로 업데이트
        List<Member> allMembers = memberRepository.findAll();
        for (Member member : allMembers) {
            member.setTeamBuilt(true);
        }
        memberRepository.saveAll(allMembers);

        // 팀별 멤버 ID 찾아서 Team 엔티티 저장
        for (TeamOutputDto dto : teams) {
            // 멤버 이름 -> 멤버 ID 리스트 생성
            List<Long> memberIds = dto.getMembers().stream()
                    .map(memberDto -> memberRepository.findByName(memberDto.getName())
                            .orElseThrow(() -> new RuntimeException("Member not found: " + memberDto.getName()))
                            .getId())
                    .collect(Collectors.toList());

            Team team = Team.builder()
                    .teamName(dto.getTeamName())
                    .memberIds(memberIds)
                    .build();

            teamRepository.save(team);
        }
        return teams;
    }

    public List<TeamOutputDto> getAllTeams() {
        List<Team> teams = teamRepository.findAll();

        // 팀빌딩 미완료 멤버 존재 여부 판단
        boolean hasUnbuiltMembers = memberRepository.findAll().stream()
                .anyMatch(m -> !m.isTeamBuilt());

        List<TeamOutputDto> result = new ArrayList<>();

        for (Team team : teams) {
            List<TeamMemberDto> members = team.getMemberIds().stream()
                    .map(id -> memberRepository.findById(id).orElse(null))
                    .filter(Objects::nonNull)
                    .map(member -> TeamMemberDto.builder()
                            .name(member.getName())
                            .mbti("mbti: " + getMbtiFull(member))
                            .drinkScore(getDrinkScore(member))
                            .hobby("취미/관심사: " + getAnswerByQuestionContent(member, "취미"))
                            .favoriteFood("좋아하는 음식: " + getAnswerByQuestionContent(member, "음식"))
                            .wildLionAnswer("야생의 사자를 만나면: " + getAnswerByQuestionContent(member, "야생"))
                            .leader(member.getId().equals(team.getLeaderId()))
                            .image(member.getImage())
                            .build())
                    .collect(Collectors.toList());

            result.add(new TeamOutputDto(team.getTeamName(), members));
        }
        return result;
    }

    //모든 팀 정보 및 멤버 팀빌딩 필드 초기화
    @Transactional
    public void clearAllTeams() {
        teamRepository.deleteAll();

        memberRepository.resetTeamBuilt();
    }
}
