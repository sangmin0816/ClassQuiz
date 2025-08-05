package com.example.lcms.entity;

import jakarta.persistence.Column;
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
public class QuizSession {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne // 하나의 퀴즈는 여러 개의 세션을 가질 수 있습니다.
    @JoinColumn(name = "quiz_id", nullable = false)
    private Quiz quiz; // 이 세션이 속한 퀴즈

    @Column(unique = true, nullable = false) // 세션 코드는 고유해야 합니다.
    private String sessionCode; // 학생들이 퀴즈에 참여할 때 사용할 고유 코드

    private LocalDateTime startTime; // 퀴즈 세션 시작 시간
    private LocalDateTime endTime; // 퀴즈 세션 종료 시간 (선택 사항)
    private boolean isActive; // 현재 세션이 활성화 상태인지 여부

    // 생성자
    public QuizSession(Quiz quiz, String sessionCode) {
        this.quiz = quiz;
        this.sessionCode = sessionCode;
        this.startTime = LocalDateTime.now();
        this.isActive = true; // 처음 생성 시 활성화 상태
    }
}