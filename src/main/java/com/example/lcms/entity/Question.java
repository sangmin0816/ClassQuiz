package com.example.lcms.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToOne; // ManyToOne 관계를 위해 추가
import jakarta.persistence.JoinColumn; // JoinColumn을 위해 추가
import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import java.time.LocalDateTime;

@Entity
@Getter @Setter
@NoArgsConstructor
@AllArgsConstructor
public class Question {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne // 여러 개의 문제가 하나의 퀴즈에 속할 수 있습니다.
    @JoinColumn(name = "quiz_id", nullable = false) // 외래 키 설정
    private Quiz quiz; // 이 문제가 속한 퀴즈

    private String content; // 문제 내용
    private String option1; // 보기 1
    private String option2; // 보기 2
    private String option3; // 보기 3
    private String option4; // 보기 4
    private int correctAnswer; // 정답 (1, 2, 3, 4 중 하나)

    private LocalDateTime createdAt; // 문제 생성일시
    private boolean isActive; // 문제 활성화 여부

    // 새 문제 생성 시 기본값 설정을 위한 생성자
    public Question(Quiz quiz, String content, String option1, String option2, String option3, String option4, int correctAnswer) {
        this.quiz = quiz;
        this.content = content;
        this.option1 = option1;
        this.option2 = option2;
        this.option3 = option3;
        this.option4 = option4;
        this.correctAnswer = correctAnswer;
        this.createdAt = LocalDateTime.now();
        this.isActive = true; // 처음 생성 시에는 활성화 상태
    }
}