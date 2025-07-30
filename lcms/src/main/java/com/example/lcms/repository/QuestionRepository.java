package com.example.lcms.repository;

import com.example.lcms.domain.Question;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List; // List 타입을 사용할 것이므로 import 합니다.
import java.util.Optional; // Optional 타입을 사용할 것이므로 import 합니다.

@Repository // 이 인터페이스가 데이터베이스 레포지토리임을 스프링에게 알려줍니다.
public interface QuestionRepository extends JpaRepository<Question, Long> {

    // --- 2. 쿼리 메서드 (우리가 직접 정의) ---

    // 2.1. 문제 내용으로 검색 (부분 일치)
    List<Question> findByContentContainingIgnoreCase(String keyword);

    // 2.2. 문제 유형(type)으로 검색
    List<Question> findByType(String type);

    // 2.3. 문제 내용과 유형을 동시에 검색
    List<Question> findByContentContainingIgnoreCaseAndType(String keyword, String type);

    // 2.4. ID와 유형으로 특정 문제를 정확히 조회
    Optional<Question> findByIdAndType(Long id, String type);

    // 2.5. 특정 정답 텍스트를 가진 문제 검색 (주관식 정답으로 문제 찾기)
    List<Question> findByCorrectAnswerText(String answerText);

    // 2.6. 특정 문제 내용을 가진 문제의 개수 세기 (중복 문제 확인 등에 활용)
    long countByContent(String content);

    // --- 3. 정렬 메서드 (선택 사항) ---

    // 3.1. 모든 문제를 ID 오름차순으로 정렬하여 조회
    List<Question> findAllByOrderByIdAsc();

    // 3.2. 문제 유형별로 조회하면서, 내용을 기준으로 오름차순 정렬
    List<Question> findByTypeOrderByContentAsc(String type);
}