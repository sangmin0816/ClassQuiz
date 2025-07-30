// Question.java (엔티티 클래스 - 데이터베이스 테이블과 매핑)
package com.example.lcms.domain;

import java.util.List;

import jakarta.persistence.*;
import lombok.Data; // Lombok 어노테이션
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

@Entity // 이 클래스가 데이터베이스 테이블임을 나타냅니다.
@Data // Lombok: getter, setter, toString 등을 자동으로 만들어줍니다.
@NoArgsConstructor // Lombok: 기본 생성자를 만들어줍니다.
@AllArgsConstructor // Lombok: 모든 필드를 사용하는 생성자를 만들어줍니다.
public class Question {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String content; // 문제 내용 (필수)

    // 1. 문제 유형 추가 (객관식, 주관식 구분)
    private String type; // ENUM으로 문제 유형을 저장할 겁니다.

    // 2. 가변적인 선지 목록 (List<String> 사용)
    @ElementCollection // 컬렉션 객체를 엔티티와 함께 저장할 때 사용
    @CollectionTable(name = "question_options", joinColumns = @JoinColumn(name = "question_id"))
    @Column(name = "option_text") // 컬렉션 테이블에 저장될 요소의 컬럼명
    private List<String> options; // 객관식 문제의 선택지 목록 (null이거나 비어있을 수 있음)

    // 3. 정답 저장 방식 변경 (주관식 고려)
    private String correctAnswerText; // 주관식 정답 또는 객관식 정답 번호 (JSON 문자열 등으로 저장
}