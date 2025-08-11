package com.example.lcms.websocket;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class AnswerMessage {
    private Long quizSessionId;
    private Long questionId;
    private Long quizAttemptId; // 현재 퀴즈 응시 ID
    private String selectedAnswerContent; // 학생이 선택한 답변 내용 (텍스트)
    private Long responseTimeMillis; // 답변 제출에 걸린 시간 (밀리초)
}
