package com.likelion.backend.service;

import com.likelion.backend.domain.Member;
import com.likelion.backend.domain.QuestionResult;
import com.likelion.backend.dto.response.TeamBuildingResponseDto;
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

    public TeamBuildingResponseDto buildTeams(int totalMembers, int teamCount) {

        // 1. 전체 멤버 불러오기, 요청 인원수만큼 제한
        List<Member> all = memberRepository.findAll();
        List<Member> members = all.subList(0, Math.min(totalMembers, all.size()));

        // 2. 각 멤버별 리더 점수(리더 지표가 낮을수록 우선 배정) 계산
        Map<Long, Integer> leaderScoreMap = new HashMap<>();
        for (Member m : members) {
            leaderScoreMap.put(m.getId(), getLeaderScore(m));
        }

        // 3. 리더 후보 정렬 (점수 낮은 순, 동점시 id 오름차순)
        List<Member> leaderCandidates = members.stream()
                .sorted(Comparator.comparingInt((Member m) -> leaderScoreMap.get(m.getId()))
                        .thenComparing(Member::getId))
                .collect(Collectors.toList());

        // 4. MBTI별 그룹핑 (MBTI 첫 글자 E/I)
        Map<Boolean, List<Member>> mbtiGroups = leaderCandidates.stream()
                .collect(Collectors.groupingBy(m -> "E".equalsIgnoreCase(getMbtiFirstLetter(m))));

        List<Member> eGroup = mbtiGroups.getOrDefault(true, new ArrayList<>());
        List<Member> iGroup = mbtiGroups.getOrDefault(false, new ArrayList<>());

        // 5. 음주 선호도 추출 및 그룹별 내림차순 정렬, 동점 시 id 오름차순
        Comparator<Member> drinkComparator = Comparator
                .comparingInt(this::getDrinkScore).reversed()
                .thenComparing(Member::getId);

        eGroup.sort(drinkComparator);
        iGroup.sort(drinkComparator);

        // 6. E/I 그룹 페어링 (숫자 작은 인덱스끼리 쌍, 남은 경우 같은 그룹내 음주선호도 근접한 사람끼리 묶음)
        List<List<Member>> pairs = new ArrayList<>();
        int pairCount = Math.min(eGroup.size(), iGroup.size());

        for (int i = 0; i < pairCount; i++) {
            pairs.add(Arrays.asList(eGroup.get(i), iGroup.get(i)));
        }

        // 7. 남은 멤버 별 그룹 내 페어링을 위해 각각 남은 리스트 생성
        List<Member> eRemain = eGroup.subList(pairCount, eGroup.size());
        List<Member> iRemain = iGroup.subList(pairCount, iGroup.size());

        for (int i = 0; i < Math.max(eRemain.size(), iRemain.size()); i++) {
            if (i < eRemain.size() && i < iRemain.size()) {
                pairs.add(Arrays.asList(eRemain.get(i), iRemain.get(i)));
            } else if (i < eRemain.size()) {
                pairs.add(Collections.singletonList(eRemain.get(i)));
            } else if (i < iRemain.size()) {
                pairs.add(Collections.singletonList(iRemain.get(i)));
            }
        }

        // 8. 리더 우선 배정 단독 페어 분리
        List<PairUnit> units = new ArrayList<>();

        Set<Long> pairedIds = pairs.stream()
                .flatMap(List::stream)
                .map(Member::getId)
                .collect(Collectors.toSet());

        // 리더 우선 배정 (리더 점수 낮은 순으로)
        for (Member m : leaderCandidates) {
            if (!pairedIds.contains(m.getId())) {
                units.add(new PairUnit(Collections.singletonList(m),
                        leaderScoreMap.get(m.getId())));
            }
        }

        // 페어 단위 생성 (각 페어의 최대 음주점수 기준 정렬용)
        for (List<Member> pair : pairs) {
            int maxDrinkScore = pair.stream()
                    .mapToInt(this::getDrinkScore)
                    .max()
                    .orElse(0);
            units.add(new PairUnit(pair, maxDrinkScore));
        }

        // 9. 유닛별 음주선호도 내림차순, 동점시 리더 점수 낮은 순, 그 다음 id 오름차순 정렬
        units.sort(Comparator
                .comparingInt(PairUnit::getMaxDrinkScore).reversed()
                .thenComparing(unit -> unit.getMembers().stream()
                        .mapToInt(m -> leaderScoreMap.getOrDefault(m.getId(), Integer.MAX_VALUE))
                        .min().orElse(Integer.MAX_VALUE))
                .thenComparing(unit -> unit.getMembers().get(0).getId()));

        // 10. teams에 라운드로빈 배분 (리더가 앞에 오도록 각 유닛 내 정렬)
        List<List<Long>> teams = new ArrayList<>();
        for (int i = 0; i < teamCount; i++) {
            teams.add(new ArrayList<>());
        }
        int idx = 0;
        for (PairUnit unit : units) {
            List<Member> membersUnit = new ArrayList<>(unit.getMembers());
            // 리더 점수가 가장 낮은 멤버를 맨 앞에 배치 (팀장)
            membersUnit.sort(Comparator
                    .comparingInt(m -> leaderScoreMap.getOrDefault(m.getId(), Integer.MAX_VALUE)));
            List<Long> memberIds = membersUnit.stream()
                    .map(Member::getId)
                    .collect(Collectors.toList());
            teams.get(idx % teamCount).addAll(memberIds);
            idx++;
        }

        // 11. 팀 내 멤버 정렬 (리더는 이미 맨 앞)
        for (List<Long> team : teams) {
            if (team.size() > 1) {
                Long leaderId = team.get(0);
                List<Long> rest = new ArrayList<>(team.subList(1, team.size()));
                Collections.sort(rest);
                team.clear();
                team.add(leaderId);
                team.addAll(rest);
            }
        }

        return new TeamBuildingResponseDto(teams);
    }

    private class PairUnit {
        private final List<Member> members;
        private final int maxDrinkScore;

        public PairUnit(List<Member> members, int maxDrinkScore) {
            this.members = members;
            this.maxDrinkScore = maxDrinkScore;
        }

        public List<Member> getMembers() {
            return members;
        }

        public int getMaxDrinkScore() {
            return maxDrinkScore;
        }
    }

    // 리더 점수 추출 (예: 리더 관련 질문 답변에서 숫자 추출, 없으면 큰 값)
    private int getLeaderScore(Member member) {
        List<QuestionResult> results = questionResultRepository.findByMember(member);
        for (QuestionResult qr : results) {
            if (qr.getQuestion() != null && qr.getQuestion().getContent().contains("리더")) {
                try {
                    return Integer.parseInt(qr.getAnswer());
                } catch (Exception ignored) {
                }
            }
        }
        return Integer.MAX_VALUE;
    }

    // mbti 첫 글자 추출 (QuestionResult에서 "MBTI" 포함 질문 답변 가정)
    private String getMbtiFirstLetter(Member member) {
        List<QuestionResult> results = questionResultRepository.findByMember(member);
        for (QuestionResult qr : results) {
            if (qr.getQuestion() != null && qr.getQuestion().getContent().contains("MBTI")) {
                String ans = qr.getAnswer();
                if (ans != null && ans.length() >= 1) {
                    return ans.substring(0, 1).toUpperCase();
                }
            }
        }
        return null;
    }

    // 음주 선호도 점수 추출 (QuestionResult에서 "음주" 관련 질문 답변 숫자)
    private int getDrinkScore(Member member) {
        List<QuestionResult> results = questionResultRepository.findByMember(member);
        for (QuestionResult qr : results) {
            if (qr.getQuestion() != null && qr.getQuestion().getContent().contains("음주")) {
                try {
                    return Integer.parseInt(qr.getAnswer());
                } catch (Exception ignored) {
                }
            }
        }
        return 0;
    }
}
