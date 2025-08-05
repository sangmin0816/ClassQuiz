package com.example.lcms.controller;

import com.example.lcms.entity.User;
import com.example.lcms.repository.UserRepository;
import org.springframework.security.crypto.password.PasswordEncoder; // 이 부분을 import 해주세요.
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.ModelAttribute;

@Controller
public class UserController {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder; // PasswordEncoder를 추가합니다.

    // 생성자를 통해 의존성 주입
    public UserController(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder; // 초기화합니다.
    }

    // ... 기존 코드
    @GetMapping("/login")
    public String loginForm() {
        return "login";
    }

    @GetMapping("/signup")
    public String signupForm() {
        return "signup";
    }

    @PostMapping("/signup")
    public String registerUser(@ModelAttribute User user) {
        // 비밀번호를 암호화하여 저장
        String encodedPassword = passwordEncoder.encode(user.getPassword());
        user.setPassword(encodedPassword);

        userRepository.save(user);
        return "redirect:/login";
    }
}