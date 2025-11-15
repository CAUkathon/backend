package com.likelion.backend.domain;

import jakarta.persistence.*;
import lombok.*;

import java.util.List;

@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Team {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String teamName;

    @ElementCollection
    private List<Long> memberIds; // 팀 멤버 id 리스트

    public Long getLeaderId() {
        if (memberIds == null || memberIds.isEmpty()) {
            return null;
        }
        // 리더가 항상 팀 멤버 리스트 첫 번째에 있다고 가정할 경우
        return memberIds.get(0);
    }
}
