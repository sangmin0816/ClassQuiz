// QuizController.java (웹 요청을 처리하는 컨트롤러)
package com.example.lcms.controller;

import com.example.lcms.domain.Question;
import com.example.lcms.repository.QuestionRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController // RESTful API를 만들기 위한 컨트롤러
@RequestMapping("/api/quizzes") // 기본 경로
public class QuizController {

    @Autowired // QuestionRepository를 주입받습니다.
    private QuestionRepository questionRepository;

    @GetMapping("/questions")
    public List<Question> getAllQuestions() {
        return questionRepository.findAll();
    }

    @PostMapping("/questions")
    public Question createQuestion(@RequestBody Question question) {
        return questionRepository.save(question);
    }

    // 퀴즈 제출, 점수 계산 등의 로직은 Service 계층에서 처리하는 것이 좋습니다.
}