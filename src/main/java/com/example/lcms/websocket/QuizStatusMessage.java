package com.example.lcms.websocket;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class QuizStatusMessage {
    private Long quizSessionId;
    private String status; // 예: "ACTIVE", "PAUSED", "ENDED"
    private Long currentQuestionId;
    private Integer currentQuestionNumber;
    private Integer totalQuestions;
    private Long timeLeftSeconds; // 남은 시간 (초)
    private String message; // 추가 메시지
}
