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

    @ManyToOne
    @JoinColumn(name = "user_id", nullable = false)
    private User user; // 퀴즈를 응시한 학생

    @ManyToOne
    @JoinColumn(name = "quiz_id", nullable = false)
    private Quiz quiz; // 응시한 퀴즈

    @ManyToOne // 어떤 퀴즈 세션에 참여했는지 (선생님이 시작한 특정 퀴즈)
    @JoinColumn(name = "quiz_session_id", nullable = false)
    private QuizSession quizSession;

    private LocalDateTime startTime; // 퀴즈 응시 시작 시간
    private LocalDateTime endTime; // 퀴즈 응시 종료 시간 (퀴즈 완료 시)

    private Integer score; // 최종 점수
    private Integer correctAnswers; // 맞은 문제 수
    private Integer totalQuestions; // 총 문제 수

    // 응시 시작 시 초기화하는 생성자
    public QuizAttempt(User user, Quiz quiz, QuizSession quizSession, Integer totalQuestions) {
        this.user = user;
        this.quiz = quiz;
        this.quizSession = quizSession;
        this.startTime = LocalDateTime.now();
        this.score = 0; // 초기 점수 0
        this.correctAnswers = 0; // 초기 맞은 개수 0
        this.totalQuestions = totalQuestions; // 총 문제 수 설정
    }

    // 정답률 계산 메서드
    public double getAccuracyRate() {
        if (totalQuestions == null || totalQuestions == 0) {
            return 0.0;
        }
        return (double) correctAnswers / totalQuestions * 100;
    }
}