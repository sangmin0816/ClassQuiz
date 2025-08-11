package com.example.lcms.repository;

import com.example.lcms.entity.QuizAttempt;
import com.example.lcms.entity.User;
import com.example.lcms.entity.Quiz;
import com.example.lcms.entity.QuizSession; // QuizSession 임포트 추가

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface QuizAttemptRepository extends JpaRepository<QuizAttempt, Long> {
    List<QuizAttempt> findByUser(User user);
    List<QuizAttempt> findByQuiz(Quiz quiz);

    // 새로 추가된 메서드들
    Optional<QuizAttempt> findByUserAndQuizSessionAndEndTimeIsNull(User user, QuizSession quizSession);
    Optional<QuizAttempt> findByUserAndQuizAndEndTimeIsNull(User user, Quiz quiz);
}
