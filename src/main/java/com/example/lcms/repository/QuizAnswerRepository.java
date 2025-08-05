package com.example.lcms.repository;

import com.example.lcms.entity.QuizAnswer;
import com.example.lcms.entity.QuizAttempt; // QuizAttempt 엔티티 import
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface QuizAnswerRepository extends JpaRepository<QuizAnswer, Long> {
    // 특정 퀴즈 응시(QuizAttempt)에 대한 모든 답변 기록을 찾는 메서드
    List<QuizAnswer> findByQuizAttempt(QuizAttempt quizAttempt);
}