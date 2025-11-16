package com.likelion.backend.service;

import com.likelion.backend.domain.Member;
import com.likelion.backend.domain.Team;
import com.likelion.backend.dto.response.TeamMemberDto;
import com.likelion.backend.dto.response.TeamOutputDto;
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

        // 성별 분리 및 정렬 추가
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

        // 7. 팔로워 최대한 균등 배분 - 라운드로빈 방식 (성별+MBTI 고려했음)
        List<Member> remain = new ArrayList<>();
        remain.addAll(eMaleGroup);
        remain.addAll(eFemaleGroup);
        remain.addAll(iMaleGroup);
        remain.addAll(iFemaleGroup);

        int[] slot = new int[teamCount];
        for (int i = 0; i < teamCount; i++) {
            slot[i] = baseCount + (i < remainder ? 1 : 0) - 1; // 리더 자리 제외
        }

        while (!remain.isEmpty()) {
            Member bestMember = null;
            int bestTeamIdx = -1;
            double bestScore = Double.MAX_VALUE;

            for (Member m : remain) {
                String teamMbtiFirstLetter = getMbtiFirstLetter(m);
                int eOrI = ("E".equalsIgnoreCase(teamMbtiFirstLetter)) ? 1 : -1;
                String gender = m.getGender();
                int drinkScore = getDrinkScore(m);

                for (int t = 0; t < teamCount; t++) {
                    if (slot[t] <= 0) continue;

                    List<TeamMemberDto> tmp = new ArrayList<>(teamsMembers.get(t));
                    tmp.add(toDto(m, false));

                    int eCount = (int) tmp.stream().filter(mem -> "E".equalsIgnoreCase(mem.getMbti())).count();
                    int iCount = tmp.size() - eCount;
                    long maleCount = tmp.stream().filter(mem -> "남자".equalsIgnoreCase(
                            memberRepository.findByName(mem.getName()).map(Member::getGender).orElse(""))).count();
                    long femaleCount = tmp.size() - maleCount;

                    double avgDrink = tmp.stream().mapToInt(TeamMemberDto::getDrinkScore).average().orElse(0);
                    double drinkGap = Math.abs(avgDrink - drinkScore);

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
                break;
            }
        }

        // 8. 남은 멤버 처리 - 음주 점수 내림차순으로 정렬
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

        List<Member> remainingMembers = new ArrayList<>();
        if (!remain.isEmpty()) {
            remainingMembers.addAll(remain);
        }
        remainingMembers.sort(drinkDescThenId);

        // 팀별 성별 인원 수 집계 함수
        Function<List<TeamMemberDto>, Map<String, Long>> genderCountInTeam = teamList -> teamList.stream()
                .collect(Collectors.groupingBy(tm -> {
                    return memberRepository.findByName(tm.getName())
                            .map(Member::getGender)
                            .orElse("unknown");
                }, Collectors.counting()));

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
    //팀을 빌딩하고 결과를 DB에 저장하는 메서드
    @Transactional
    public List<TeamOutputDto> buildAndSaveTeams(int totalMembers, int teamCount) {
        List<TeamOutputDto> teams = buildBalancedTeams(totalMembers, teamCount);

        // 저장 전에 기존 팀 데이터 삭제 또는 필요한 초기화 실시 가능
        teamRepository.deleteAll();

        for (TeamOutputDto dto : teams) {
            // 멤버 이름 -> 멤버 ID 리스트 생성
            List<Long> memberIds = dto.getMembers().stream()
                    .map(memberDto -> memberRepository.findByName(memberDto.getName())
                            .orElseThrow(() -> new RuntimeException("Member not found: " + memberDto.getName()))
                            .getId())
                    .collect(Collectors.toList());

            // 리더 ID 구하기 (leader 필드 true인 멤버)
            Optional<Long> leaderIdOpt = dto.getMembers().stream()
                    .filter(TeamMemberDto::isLeader)
                    .map(memberDto -> memberRepository.findByName(memberDto.getName())
                            .orElseThrow(() -> new RuntimeException("Leader not found: " + memberDto.getName()))
                            .getId())
                    .findFirst();

            Long leaderId = leaderIdOpt.orElse(null);

            Team team = Team.builder()
                    .teamName(dto.getTeamName())
                    .memberIds(memberIds)
                    .build();

            teamRepository.save(team);
        }
        return teams;
    }

    public List<TeamOutputDto> buildTeams(int totalMembers, int teamCount) {
        // 아래 메서드를 호출하면 팀 빌딩이랑 DB 저장을 동시에 수행함
        return buildAndSaveTeams(totalMembers, teamCount);
    }

    public List<TeamOutputDto> getAllTeams() {
        List<Team> teams = teamRepository.findAll();

        List<TeamOutputDto> result = new ArrayList<>();

        for (Team team : teams) {
            // 팀 멤버 리스트 생성 (멤버 엔티티 조회)
            List<TeamMemberDto> members = team.getMemberIds().stream()
                    .map(id -> memberRepository.findById(id).orElse(null))
                    .filter(Objects::nonNull)
                    .map(member -> TeamMemberDto.builder()
                            .name(member.getName())
                            .mbti(getMbtiFirstLetter(member))
                            .drinkScore(getDrinkScore(member))
                            // 리더 여부는 팀 빌딩 시 저장된 데이터 통해 찾았음
                            .leader(member.getId().equals(team.getLeaderId()))
                            .build())
                    .collect(Collectors.toList());

            // 팀명과 멤버 리스트로 DTO 생성
            result.add(new TeamOutputDto(team.getTeamName(), members));
        }
        return result;
    }
}
