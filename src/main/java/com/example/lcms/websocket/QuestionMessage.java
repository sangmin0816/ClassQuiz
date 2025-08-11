package com.example.lcms.websocket;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class QuestionMessage {
    private Long quizSessionId;
    private Long questionId;
    private String content; // 문제 내용
    private List<String> options; // 섞인 보기 목록
    private Integer questionNumber; // 현재 문제 번호
    private Integer totalQuestions; // 총 문제 수
    private Integer timeLimitSeconds; // 이 문제의 시간 제한
}
