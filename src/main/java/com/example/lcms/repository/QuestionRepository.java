package com.example.lcms.repository;

import com.example.lcms.entity.Question;
import com.example.lcms.entity.Quiz;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface QuestionRepository extends JpaRepository<Question, Long> {
    List<Question> findByQuiz(Quiz quiz);

    // 퀴즈와 활성화 여부를 기준으로 문제 목록을 조회하는 메서드 추가
    List<Question> findByQuizAndIsActiveTrue(Quiz quiz);

    // 특정 퀴즈에 속한 문제의 개수를 세는 메서드 (TeacherController에서 사용)
    long countByQuiz(Quiz quiz);
}
