package com.example.lcms.websocket;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class QuizMessage {
    private String type; // 예: "START", "NEXT_QUESTION", "END"
    private Long quizSessionId;
    private String message; // 메시지 내용
    private Long currentQuestionId; // 현재 문제 ID (선택 사항)
    private Integer currentQuestionNumber; // 현재 문제 번호 (선택 사항)
    private Integer totalQuestions; // 총 문제 수 (선택 사항)
    private Integer timeLimitSeconds; // 문제당 시간 제한 (선택 사항)
}
