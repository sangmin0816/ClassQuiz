package com.example.lcms.repository;

import com.example.lcms.entity.QuizSession;
import com.example.lcms.entity.Quiz;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface QuizSessionRepository extends JpaRepository<QuizSession, Long> {
    Optional<QuizSession> findBySessionCode(String sessionCode);
    Optional<QuizSession> findByQuizAndIsActiveTrue(Quiz quiz);

    // 새로 추가된 메서드
    Optional<QuizSession> findBySessionCodeAndIsActiveTrue(String sessionCode);
}

