package com.example.lcms.service;

import com.example.lcms.domain.Question;
import com.example.lcms.repository.QuestionRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional; // Optional 임포트 추가

@Service
public class QuizService {

    private final QuestionRepository questionRepository;

    public QuizService(QuestionRepository questionRepository) {
        this.questionRepository = questionRepository;
    }

    // 새로운 문제를 생성하고 데이터베이스에 저장하는 메서드
    public Question createQuestion(Question question) {
        if (question.getType().equals("MULTIPLE_CHOICE") && (question.getOptions() == null || question.getOptions().size() < 2 || question.getOptions().size() > 10)) {
            throw new IllegalArgumentException("객관식 문제는 2개에서 10개 사이의 선지를 가져야 합니다.");
        }
        if (question.getContent() == null || question.getContent().trim().isEmpty()) {
            throw new IllegalArgumentException("문제 내용은 필수입니다.");
        }
        return questionRepository.save(question);
    }

    // 모든 문제를 데이터베이스에서 불러오는 메서드
    public List<Question> getAllQuestions() {
        return questionRepository.findAll();
    }

    // 특정 ID의 문제를 불러오는 메서드
    public Question getQuestionById(Long id) {
        return questionRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("해당 ID의 문제가 없습니다: " + id));
    }

    // 문제 업데이트
    public Question updateQuestion(Long id, Question updatedQuestion) {
        Question existingQuestion = questionRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("업데이트할 해당 ID의 문제가 없습니다: " + id));

        existingQuestion.setContent(updatedQuestion.getContent());
        existingQuestion.setType(updatedQuestion.getType());
        existingQuestion.setOptions(updatedQuestion.getOptions());
        existingQuestion.setCorrectAnswerText(updatedQuestion.getCorrectAnswerText());
        // 필요하다면 correctAnswerIndex도 업데이트 (만약 추가했다면)
        // existingQuestion.setCorrectAnswerIndex(updatedQuestion.getCorrectAnswerIndex());

        return questionRepository.save(existingQuestion);
    }

    public void deleteQuestion(Long id) {
        if (!questionRepository.existsById(id)) {
            throw new IllegalArgumentException("삭제할 해당 ID의 문제가 없습니다: " + id);
        }
        questionRepository.deleteById(id);
    }

    // 특정 유형의 문제만 가져오는 메서드
    public List<Question> getQuestionsByType(String type) {
        return questionRepository.findByType(type);
    }

    // 검색어로 문제 검색
    public List<Question> searchQuestionsByKeyword(String keyword) {
        return questionRepository.findByContentContainingIgnoreCase(keyword);
    }
}