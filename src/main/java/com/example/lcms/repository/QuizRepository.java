package com.example.lcms.repository;

import com.example.lcms.entity.Quiz;
import org.springframework.data.jpa.repository.JpaRepository;

public interface QuizRepository extends JpaRepository<Quiz, Long> {
    // JpaRepository를 상속받았기 때문에 기본적인 CRUD(생성, 조회, 수정, 삭제) 기능은 자동으로 제공됩니다.
    // 필요하다면 여기에 추가적인 퀴즈 조회 메서드를 정의할 수 있습니다.
}