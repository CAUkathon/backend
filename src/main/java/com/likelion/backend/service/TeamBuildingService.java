package com.likelion.backend.service;

import com.likelion.backend.domain.Member;
import com.likelion.backend.dto.response.TeamMemberDto;
import com.likelion.backend.dto.response.TeamOutputDto;
import com.likelion.backend.repository.MemberRepository;
import com.likelion.backend.repository.QuestionResultRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class TeamBuildingService {

    private final MemberRepository memberRepository;
    private final QuestionResultRepository questionResultRepository;

    public List<TeamOutputDto> buildBalancedTeams(int totalMembers, int teamCount) {

        List<Member> allMembers = memberRepository.findAll();
        List<Member> members = allMembers.subList(0, Math.min(totalMembers, allMembers.size()));

        // 1. 리더 점수로 리더 선발 <- 질문 1 답이 낮을수록 리더형!
        Map<Member, Integer> leaderScoreMap = new HashMap<>();
        for (Member m : members) {
            leaderScoreMap.put(m, getLeaderScore(m));
        }

        List<Member> leaders = leaderScoreMap.entrySet().stream()
                .sorted(Map.Entry.<Member, Integer>comparingByValue()
                        .thenComparing(e -> e.getKey().getId()))
                .limit(teamCount)
                .map(Map.Entry::getKey).collect(Collectors.toList());

        // 2. 리더 음주 점수 추출 & 리더 정렬 (음주 내림차순) <- 질문 2 답이 높을수록 애주가
        leaders.sort((a, b) -> Integer.compare(getDrinkScore(b), getDrinkScore(a)));

        // 3. 리더 제외 나머지 멤버 분리
        List<Member> followers = new ArrayList<>(members);
        followers.removeAll(leaders);

        // 4. MBTI E/I 분류 및 음주 점수 내림차순 정렬
        Map<Boolean, List<Member>> grouped = followers.stream()
                .collect(Collectors.groupingBy(m -> "E".equalsIgnoreCase(getMbtiFirstLetter(m))));
        List<Member> eGroup = grouped.getOrDefault(true, new ArrayList<>());
        List<Member> iGroup = grouped.getOrDefault(false, new ArrayList<>());

        Comparator<Member> drinkDescThenId = Comparator.comparingInt(this::getDrinkScore).reversed()
                .thenComparing(Member::getId);

        eGroup.sort(drinkDescThenId);
        iGroup.sort(drinkDescThenId);

        // 5. 팀별 균등 인원 할당 준비
        int baseCount = totalMembers / teamCount;
        int remainder = totalMembers % teamCount;

        List<List<TeamMemberDto>> teamsMembers = new ArrayList<>();
        for (int i = 0; i < teamCount; i++) {
            teamsMembers.add(new ArrayList<>());
        }

        // 6. 리더 우선 팀배정 (맨 앞에 있도록 했음)
        for (int i = 0; i < teamCount; i++) {
            Member leader = leaders.get(i);
            teamsMembers.get(i).add(TeamMemberDto.builder()
                    .name(leader.getName())
                    .mbti(getMbtiFirstLetter(leader))
                    .drinkScore(getDrinkScore(leader))
                    .leader(true)
                    .build());
        }

        // 7. 팔로워 최대한 균등 배분 (E/I 그룹 교차로 각 팀에)
        int[] slots = new int[teamCount];
        for (int i = 0; i < teamCount; i++) {
            // 리더 자리 뺀 인원
            slots[i] = baseCount + (i < remainder ? 1 : 0) - 1;
        }

        int eIndex = 0, iIndex = 0;
        for (int i = 0; i < teamCount; i++) {
            List<TeamMemberDto> teamList = teamsMembers.get(i);
            int remain = slots[i];

            while (remain > 0 && eIndex < eGroup.size()) {
                Member m = eGroup.get(eIndex++);
                teamList.add(toDto(m, false));
                remain--;
            }

            while (remain > 0 && iIndex < iGroup.size()) {
                Member m = iGroup.get(iIndex++);
                teamList.add(toDto(m, false));
                remain--;
            }
        }

        // 8. 남은 멤버 처리 - 음주 점수 내림차순으로 정렬
        List<Member> remainingMembers = new ArrayList<>();
        if (eIndex < eGroup.size()) {
            remainingMembers.addAll(eGroup.subList(eIndex, eGroup.size()));
        }
        if (iIndex < iGroup.size()) {
            remainingMembers.addAll(iGroup.subList(iIndex, iGroup.size()));
        }
        remainingMembers.sort(drinkDescThenId);

        // 팀별 현재 음주점수 평균 계산
        double[] teamDrinkAverages = new double[teamCount];
        for (int i = 0; i < teamCount; i++) {
            List<TeamMemberDto> teamList = teamsMembers.get(i);
            double sum = 0;
            for (TeamMemberDto m : teamList) {
                sum += m.getDrinkScore();
            }
            teamDrinkAverages[i] = teamList.isEmpty() ? 0 : sum / teamList.size();
        }

        // 남은 멤버를 음주 점수가 가장 근접한 팀에 순차 배정
        for (Member m : remainingMembers) {
            int drinkScore = getDrinkScore(m);
            // 음주점수 거리가 최소인 팀 선택
            int minIndex = 0;
            double minDiff = Math.abs(teamDrinkAverages[0] - drinkScore);

            for (int i = 1; i < teamCount; i++) {
                double diff = Math.abs(teamDrinkAverages[i] - drinkScore);
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

        // 9. 최종 팀 Output DTO 생성
        List<TeamOutputDto> result = new ArrayList<>();
        for (int i = 0; i < teamCount; i++) {
            result.add(new TeamOutputDto("Team " + (i + 1), teamsMembers.get(i)));
        }

        return result;
    }

    private TeamMemberDto toDto(Member m, boolean leader) {
        return TeamMemberDto.builder()
                .name(m.getName())
                .mbti(getMbtiFirstLetter(m))
                .drinkScore(getDrinkScore(m))
                .leader(leader)
                .build();
    }

    private int getLeaderScore(Member member) {
        return questionResultRepository.findByMember(member).stream()
                .filter(qr -> qr.getQuestion()!=null && qr.getQuestion().getContent().contains("리더"))
                .map(qr -> {
                    try { return Integer.parseInt(qr.getAnswer()); }
                    catch(Exception e){return Integer.MAX_VALUE;}
                })
                .findFirst().orElse(Integer.MAX_VALUE);
    }

    private String getMbtiFirstLetter(Member member) {
        return questionResultRepository.findByMember(member).stream()
                .filter(qr -> qr.getQuestion()!=null && qr.getQuestion().getContent().contains("MBTI"))
                .map(qr -> {
                    String ans = qr.getAnswer();
                    return (ans!=null && ans.length()>=1) ? ans.substring(0,1).toUpperCase() : "";
                }).findFirst().orElse("");
    }

    private int getDrinkScore(Member member) {
        return questionResultRepository.findByMember(member).stream()
                .filter(qr -> qr.getQuestion()!=null && qr.getQuestion().getContent().contains("회식"))
                .map(qr -> {
                    try { return Integer.parseInt(qr.getAnswer()); }
                    catch(Exception e){return 0;}
                }).findFirst().orElse(0);
    }
    public List<TeamOutputDto> buildTeams(int totalMembers, int teamCount) {
        return buildBalancedTeams(totalMembers, teamCount);
    }

}
