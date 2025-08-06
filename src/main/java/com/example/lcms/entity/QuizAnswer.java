package com.example.lcms.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.JoinColumn;
import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import java.time.LocalDateTime;

@Entity
@Getter @Setter
@NoArgsConstructor
@AllArgsConstructor
public class QuizAnswer {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "attempt_id", nullable = false)
    private QuizAttempt quizAttempt; // 이 답변이 속한 퀴즈 응시

    @ManyToOne
    @JoinColumn(name = "question_id", nullable = false)
    private Question question; // 답변한 문제

    // 학생이 선택한 보기의 내용(String)을 저장하도록 변경
    private String selectedAnswerContent; 
    private boolean isCorrect; // 정답 여부

    private LocalDateTime answerTime; // 답변 제출 시간
    private Long responseTimeMillis; // 문제에 답변하는 데 걸린 시간 (밀리초)

    // 생성자 (selectedOption 대신 selectedAnswerContent 사용)
    public QuizAnswer(QuizAttempt quizAttempt, Question question, String selectedAnswerContent, boolean isCorrect, Long responseTimeMillis) {
        this.quizAttempt = quizAttempt;
        this.question = question;
        this.selectedAnswerContent = selectedAnswerContent;
        this.isCorrect = isCorrect;
        this.answerTime = LocalDateTime.now();
        this.responseTimeMillis = responseTimeMillis;
    }
}
