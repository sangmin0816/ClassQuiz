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
public class Question {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "quiz_id", nullable = false)
    private Quiz quiz; // 이 문제가 속한 퀴즈

    private String content; // 문제 내용
    private String option1; // 보기 1
    private String option2; // 보기 2
    private String option3; // 보기 3
    private String option4; // 보기 4

    // 정답을 보기의 내용(String)으로 저장하도록 변경
    private String correctAnswer;

    private boolean isActive; // 문제 활성화 여부

    // 생성자 (isActive 필드 제외)
    public Question(Quiz quiz, String content, String option1, String option2, String option3, String option4, String correctAnswer) {
        this.quiz = quiz;
        this.content = content;
        this.option1 = option1;
        this.option2 = option2;
        this.option3 = option3;
        this.option4 = option4;
        this.correctAnswer = correctAnswer;
        this.isActive = true; // 기본적으로 활성화
    }
}
