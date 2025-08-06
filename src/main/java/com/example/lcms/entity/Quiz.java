package com.example.lcms.entity;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import java.time.LocalDateTime; // 날짜/시간 정보를 위해 추가
import java.util.List;

@Entity
@Getter @Setter
@NoArgsConstructor
@AllArgsConstructor
public class Quiz {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String title; // 퀴즈 제목
    private String description; // 퀴즈 설명

    private LocalDateTime createdAt; // 퀴즈 생성일시
    private boolean isActive; // 퀴즈 활성화 여부 (시작/종료)

    @OneToMany(mappedBy = "quiz", cascade = CascadeType.ALL, fetch = FetchType.LAZY, orphanRemoval = true)
    private List<Question> questions; // 해당 퀴즈에 속한 문제들

    // 퀴즈가 생성될 때 자동으로 생성일시를 설정하기 위한 메서드
    // (JPA Life Cycle Callback을 사용해도 되지만, 일단 간단하게 생성자에서 설정)
    public Quiz(String title, String description) {
        this.title = title;
        this.description = description;
        this.createdAt = LocalDateTime.now();
        this.isActive = false; // 처음 생성 시에는 비활성화 상태
    }
}