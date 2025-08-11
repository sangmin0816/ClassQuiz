package com.example.lcms.repository;

import com.example.lcms.entity.QuizAnswer;
import com.example.lcms.entity.QuizAttempt;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface QuizAnswerRepository extends JpaRepository<QuizAnswer, Long> {
    List<QuizAnswer> findByQuizAttempt(QuizAttempt quizAttempt);

    // 새로 추가된 메서드
    long countByQuizAttempt(QuizAttempt quizAttempt);
}

