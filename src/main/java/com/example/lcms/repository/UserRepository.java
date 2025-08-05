package com.example.lcms.repository;

import com.example.lcms.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {
    // 특정 아이디(username)로 회원을 찾는 메서드
    Optional<User> findByUsername(String username);
}