package com.example.lcms.repository;

import com.example.lcms.entity.QuizAttempt;
import com.example.lcms.entity.QuizSession;
import com.example.lcms.entity.User;
import com.example.lcms.entity.Quiz; // Quiz 엔티티 import
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface QuizAttemptRepository extends JpaRepository<QuizAttempt, Long> {
    // 특정 퀴즈(Quiz)에 대한 모든 응시 기록을 찾는 메서드
    List<QuizAttempt> findByQuiz(Quiz quiz);
    // 특정 학생(User)의 모든 퀴즈 응시 기록을 찾는 메서드 (나중에 학생 대시보드에서 유용)
    List<QuizAttempt> findByUser(User user);

    // 특정 퀴즈 세션에 대한 모든 응시 기록 조회 (랭킹을 위해 사용될 수 있음)
    List<QuizAttempt> findByQuizSessionOrderByScoreDescResponseTimeMillisAsc(QuizSession quizSession);

    // 특정 유저와 특정 퀴즈 세션에 대한 응시 기록이 있는지 확인 (중복 응시 방지 등)
    Optional<QuizAttempt> findByUserAndQuizSession(User user, QuizSession quizSession);
}