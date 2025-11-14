package com.likelion.backend.repository;

import com.likelion.backend.domain.Member;
import com.likelion.backend.domain.QuestionResult;
import org.springframework.data.jpa.repository.JpaRepository;

public interface QuestionResultRepository extends JpaRepository<QuestionResult, Long> {

    boolean existsByMember(Member member);
}
