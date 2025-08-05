// src/main/java/com/example/lcms/repository/QuestionRepository.java 에 추가
package com.example.lcms.repository;

import com.example.lcms.entity.Question;
import com.example.lcms.entity.Quiz;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface QuestionRepository extends JpaRepository<Question, Long> {
    List<Question> findByQuiz(Quiz quiz);
    
    // 이 메서드를 추가합니다.
    Long countByQuiz(Quiz quiz); // 특정 퀴즈에 속한 문제의 개수를 세는 메서드
}