package com.likelion.backend.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

@Configuration
@EnableJpaAuditing
public class JpaConfig {
    // 엔티티의 생성/수정 시간 자동 기입 도와주는 클래스
}
