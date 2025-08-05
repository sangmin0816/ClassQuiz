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

@Entity
@Getter @Setter
@NoArgsConstructor
@AllArgsConstructor
public class QuizAnswer {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne // 여러 개의 답변이 하나의 퀴즈 응시에 속합니다.
    @JoinColumn(name = "attempt_id", nullable = false)
    private QuizAttempt quizAttempt; // 어떤 응시에 대한 답변인지

    @ManyToOne // 여러 답변이 하나의 문제에 대해 있을 수 있습니다.
    @JoinColumn(name = "question_id", nullable = false)
    private Question question; // 어떤 문제에 대한 답변인지

    private int studentAnswer; // 학생이 선택한 보기 번호 (1, 2, 3, 4)
    private boolean isCorrect; // 정답 여부

    // 생성자
    public QuizAnswer(QuizAttempt quizAttempt, Question question, int studentAnswer, boolean isCorrect) {
        this.quizAttempt = quizAttempt;
        this.question = question;
        this.studentAnswer = studentAnswer;
        this.isCorrect = isCorrect;
    }
}