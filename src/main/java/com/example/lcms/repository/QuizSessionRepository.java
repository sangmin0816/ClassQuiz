package com.example.lcms.repository;

import com.example.lcms.entity.QuizSession;
import com.example.lcms.entity.Quiz; // Quiz 엔티티 import
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;
import java.util.List;

public interface QuizSessionRepository extends JpaRepository<QuizSession, Long> {
    // 특정 퀴즈에 대한 활성화된 세션을 찾는 메서드
    Optional<QuizSession> findByQuizAndIsActiveTrue(Quiz quiz);

    // 세션 코드로 퀴즈 세션을 찾는 메서드
    Optional<QuizSession> findBySessionCode(String sessionCode);

    // 특정 퀴즈에 대한 모든 세션을 찾는 메서드 (선택 사항)
    List<QuizSession> findByQuiz(Quiz quiz);
}