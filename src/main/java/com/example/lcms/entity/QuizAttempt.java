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
public class QuizAttempt {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne // 여러 번의 응시가 하나의 퀴즈에 속합니다.
    @JoinColumn(name = "quiz_id", nullable = false)
    private Quiz quiz; // 어떤 퀴즈를 응시했는지

    @ManyToOne // 한 명의 유저가 여러 번 응시할 수 있습니다.
    @JoinColumn(name = "user_id", nullable = false)
    private User user; // 누가 응시했는지 (학생)

    private LocalDateTime startTime; // 응시 시작 시간
    private LocalDateTime endTime; // 응시 종료 시간
    private int score; // 획득 점수
    private int totalQuestions; // 총 문제 수
    private int correctAnswers; // 맞춘 문제 수

    // 생성자 (필요에 따라 추가)
    public QuizAttempt(Quiz quiz, User user, int totalQuestions, int correctAnswers, int score) {
        this.quiz = quiz;
        this.user = user;
        this.startTime = LocalDateTime.now(); // 응시 시작 시간은 생성 시점
        this.totalQuestions = totalQuestions;
        this.correctAnswers = correctAnswers;
        this.score = score;
    }

    // 정답률 계산 메서드
    public double getAccuracyRate() {
        if (totalQuestions == 0) {
            return 0.0;
        }
        return (double) correctAnswers / totalQuestions * 100;
    }
}