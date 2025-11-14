package com.likelion.backend.repository;

import com.likelion.backend.domain.Member;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface MemberRepository extends JpaRepository<Member, Long> {
    Optional<Member> findByName(String name);
    Optional<Member> findById(Long id);

    // 이름 중복 검사 쿼리
    boolean existsByName(String name);
}
