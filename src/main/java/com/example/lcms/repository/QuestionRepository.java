package com.example.lcms.repository;

import com.example.lcms.entity.Question;
import com.example.lcms.entity.Quiz;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface QuestionRepository extends JpaRepository<Question, Long> {
    List<Question> findByQuiz(Quiz quiz);

    List<Question> findByQuizAndIsActiveTrue(Quiz quiz);

    // 새롭게 추가된 메서드: 특정 퀴즈에 속한 활성화된 문제의 개수를 세는 메서드
    long countByQuizAndIsActiveTrue(Quiz quiz); // 이 메서드가 없어서 오류가 발생했습니다.

    long countByQuiz(Quiz quiz); // 기존 메서드 유지
}
