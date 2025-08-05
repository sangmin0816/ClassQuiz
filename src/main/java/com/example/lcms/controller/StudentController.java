package com.example.lcms.controller;

import com.example.lcms.entity.QuizSession;
import com.example.lcms.entity.QuizAttempt; // QuizAttempt 추가
import com.example.lcms.entity.User; // User 엔티티 추가
import com.example.lcms.repository.QuizSessionRepository;
import com.example.lcms.repository.QuizAttemptRepository; // QuizAttemptRepository 추가
import com.example.lcms.repository.UserRepository; // UserRepository 추가

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication; // Authentication 객체 사용을 위해 추가
import org.springframework.security.core.context.SecurityContextHolder; // SecurityContextHolder 사용을 위해 추가
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;
import java.util.Optional;

@Controller
@PreAuthorize("hasRole('STUDENT')")
public class StudentController {

    private final QuizSessionRepository quizSessionRepository;
    private final QuizAttemptRepository quizAttemptRepository; // QuizAttemptRepository 주입
    private final UserRepository userRepository; // UserRepository 주입

    public StudentController(QuizSessionRepository quizSessionRepository,
                             QuizAttemptRepository quizAttemptRepository, // 생성자에 추가
                             UserRepository userRepository) { // 생성자에 추가
        this.quizSessionRepository = quizSessionRepository;
        this.quizAttemptRepository = quizAttemptRepository;
        this.userRepository = userRepository;
    }

    // 학생 대시보드 메인 페이지 (퀴즈 코드 입력 폼 포함)
    @GetMapping("/student/dashboard")
    public String studentDashboard(Model model) {
        return "student_dashboard";
    }

    // 퀴즈 코드 제출 처리
    @PostMapping("/student/quiz_entry")
    public String processQuizEntry(@RequestParam String sessionCode, RedirectAttributes redirectAttributes) {
        Optional<QuizSession> sessionOptional = quizSessionRepository.findBySessionCode(sessionCode.toUpperCase());
        
        if (sessionOptional.isEmpty() || !sessionOptional.get().isActive()) {
            redirectAttributes.addFlashAttribute("errorMessage", "유효하지 않거나 종료된 퀴즈 코드입니다.");
            return "redirect:/student/dashboard";
        }

        QuizSession quizSession = sessionOptional.get();
        // TODO: 여기서 퀴즈 풀이 페이지로 리다이렉트 (다음 단계에서 구현)
        redirectAttributes.addFlashAttribute("successMessage", "'" + quizSession.getQuiz().getTitle() + "' 퀴즈에 참여할 준비가 되었습니다! (다음 단계에서 퀴즈 풀이 화면으로 이동)");
        // 임시로 퀴즈 풀이 시작을 위해 세션 ID를 넘겨줍니다.
        return "redirect:/student/quiz/play/" + quizSession.getId(); // 퀴즈 풀이 페이지로 리다이렉트
    }

    // 내 퀴즈 결과 화면
    @GetMapping("/student/my_quiz_results")
    public String myQuizResults(Model model) {
        // 현재 로그인한 사용자 정보 가져오기
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String username = authentication.getName(); // 로그인한 사용자의 아이디

        // 아이디로 User 엔티티 찾기
        Optional<User> userOptional = userRepository.findByUsername(username);
        if (userOptional.isEmpty()) {
            // 사용자를 찾을 수 없으면 에러 처리 또는 로그인 페이지로 리다이렉트
            return "redirect:/login";
        }
        User currentUser = userOptional.get();

        // 현재 사용자의 모든 퀴즈 응시 기록 가져오기
        List<QuizAttempt> myAttempts = quizAttemptRepository.findByUser(currentUser);
        model.addAttribute("myAttempts", myAttempts);

        return "student_quiz_results"; // student_quiz_results.html을 보여줍니다.
    }
}