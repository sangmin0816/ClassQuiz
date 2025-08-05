package com.example.lcms.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.security.access.prepost.PreAuthorize; // 추가

import com.example.lcms.entity.Quiz;
import com.example.lcms.entity.Question;
import com.example.lcms.entity.QuizAttempt;
import com.example.lcms.entity.QuizAnswer;
import com.example.lcms.entity.QuizSession; // QuizSession 추가

import com.example.lcms.repository.QuizRepository;
import com.example.lcms.repository.QuestionRepository;
import com.example.lcms.repository.QuizAttemptRepository;
import com.example.lcms.repository.QuizAnswerRepository;
import com.example.lcms.repository.QuizSessionRepository; // QuizSessionRepository 추가

import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.servlet.mvc.support.RedirectAttributes; // RedirectAttributes 추가

import java.time.LocalDateTime; // LocalDateTime 추가
import java.util.List;
import java.util.Optional;
import java.util.Map;
import java.util.HashMap;
import java.util.UUID; // 고유 코드 생성을 위해 추가


@Controller
public class TeacherController {
    private final QuizRepository quizRepository;
    private final QuestionRepository questionRepository;
    private final QuizAttemptRepository quizAttemptRepository;
    private final QuizAnswerRepository quizAnswerRepository;
    private final QuizSessionRepository quizSessionRepository;

    public TeacherController(QuizRepository quizRepository,
                             QuestionRepository questionRepository,
                             QuizAttemptRepository quizAttemptRepository,
                             QuizAnswerRepository quizAnswerRepository,
                             QuizSessionRepository quizSessionRepository) {
        this.quizRepository = quizRepository;
        this.questionRepository = questionRepository;
        this.quizAttemptRepository = quizAttemptRepository;
        this.quizAnswerRepository = quizAnswerRepository;
        this.quizSessionRepository = quizSessionRepository;
    }

    @GetMapping("/teacher/dashboard")
    @PreAuthorize("hasRole('TEACHER')") // TEACHER 역할만 접근 가능
    public String teacherDashboard() {
        return "teacher_dashboard"; // templates 폴더의 teacher_dashboard.html을 찾습니다.
    }

    // 퀴즈 생성 처리
    @PostMapping("/teacher/quizzes/create")
    public String createQuiz(@ModelAttribute Quiz quiz) {
        quizRepository.save(quiz); // 퀴즈를 데이터베이스에 저장합니다.
        return "redirect:/teacher/quizzes"; // 퀴즈 목록 페이지로 리다이렉트
    }

    // 퀴즈 수정 폼 보여주기 (선택 사항: 모달로 처리할 경우 이 메서드가 필요 없을 수도 있습니다)
    @GetMapping("/teacher/quizzes/edit/{id}")
    public String editQuizForm(@PathVariable Long id, Model model) {
        Quiz quiz = quizRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Invalid quiz Id:" + id));
        model.addAttribute("quiz", quiz);
        return "quiz_edit"; // quiz_edit.html (새로운 수정 폼 페이지가 필요할 경우)
    }

    // 퀴즈 수정 처리
    @PostMapping("/teacher/quizzes/update/{id}")
    public String updateQuiz(@PathVariable Long id, @ModelAttribute Quiz quiz) {
        Quiz existingQuiz = quizRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Invalid quiz Id:" + id));
        existingQuiz.setTitle(quiz.getTitle());
        existingQuiz.setDescription(quiz.getDescription());
        // createdAt은 변경하지 않습니다.
        // isActive는 별도의 토글 버튼으로 처리합니다.
        quizRepository.save(existingQuiz); // 수정된 퀴즈를 저장합니다.
        return "redirect:/teacher/quizzes";
    }

    // 퀴즈 삭제 처리
    @PostMapping("/teacher/quizzes/delete/{id}")
    public String deleteQuiz(@PathVariable Long id) {
        quizRepository.deleteById(id); // 퀴즈를 삭제합니다.
        return "redirect:/teacher/quizzes";
    }

    // 퀴즈 활성화/비활성화 토글
    @PostMapping("/teacher/quizzes/toggleActive/{id}")
    public String toggleQuizActive(@PathVariable Long id) {
        Quiz quiz = quizRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Invalid quiz Id:" + id));
        quiz.setActive(!quiz.isActive()); // 현재 상태를 반전시킵니다.
        quizRepository.save(quiz);
        return "redirect:/teacher/quizzes";
    }

    // 퀴즈 세션 시작 (고유 코드 생성)
    @PostMapping("/teacher/quizzes/{quizId}/startSession")
    public String startQuizSession(@PathVariable Long quizId, RedirectAttributes redirectAttributes) {
        Optional<Quiz> quizOptional = quizRepository.findById(quizId);
        if (quizOptional.isEmpty()) {
            redirectAttributes.addFlashAttribute("errorMessage", "해당 퀴즈를 찾을 수 없습니다.");
            return "redirect:/teacher/quizzes";
        }
        Quiz quiz = quizOptional.get();

        // 이미 활성화된 세션이 있는지 확인
        Optional<QuizSession> existingSession = quizSessionRepository.findByQuizAndIsActiveTrue(quiz);
        if (existingSession.isPresent()) {
            redirectAttributes.addFlashAttribute("errorMessage", "이미 활성화된 퀴즈 세션이 있습니다. 코드: " + existingSession.get().getSessionCode());
            return "redirect:/teacher/quizzes";
        }

        // 고유한 세션 코드 생성 (간단한 UUID 사용)
        String sessionCode = UUID.randomUUID().toString().substring(0, 8).toUpperCase(); // 8자리 코드로 생성

        // 퀴즈 세션 저장
        QuizSession quizSession = new QuizSession(quiz, sessionCode);
        quizSessionRepository.save(quizSession);

        redirectAttributes.addFlashAttribute("successMessage", "퀴즈 세션이 시작되었습니다! 코드: " + sessionCode);
        redirectAttributes.addFlashAttribute("startedQuizId", quizId); // 시작된 퀴즈 ID 전달 (UI에서 활용)
        return "redirect:/teacher/quizzes";
    }

    // 퀴즈 세션 종료
    @PostMapping("/teacher/quizzes/{sessionId}/endSession")
    public String endQuizSession(@PathVariable Long sessionId, RedirectAttributes redirectAttributes) {
        Optional<QuizSession> sessionOptional = quizSessionRepository.findById(sessionId);
        if (sessionOptional.isEmpty()) {
            redirectAttributes.addFlashAttribute("errorMessage", "해당 퀴즈 세션을 찾을 수 없습니다.");
            return "redirect:/teacher/quizzes";
        }
        QuizSession quizSession = sessionOptional.get();

        if (quizSession.isActive()) {
            quizSession.setActive(false);
            quizSession.setEndTime(LocalDateTime.now());
            quizSessionRepository.save(quizSession);
            redirectAttributes.addFlashAttribute("successMessage", "퀴즈 세션이 종료되었습니다.");
        } else {
            redirectAttributes.addFlashAttribute("errorMessage", "이미 종료된 퀴즈 세션입니다.");
        }
        return "redirect:/teacher/quizzes";
    }

    // 퀴즈 관리 페이지 (퀴즈 목록 조회) - 세션 코드 정보와 문제 개수도 함께 전달
    @GetMapping("/teacher/quizzes")
    public String quizManagement(Model model) {
        List<Quiz> quizzes = quizRepository.findAll();
        model.addAttribute("quizzes", quizzes);
        model.addAttribute("newQuiz", new Quiz());

        Map<Long, String> activeSessionCodes = new HashMap<>();
        Map<Long, Long> activeSessionIds = new HashMap<>();
        Map<Long, Long> questionCounts = new HashMap<>(); // 각 퀴즈의 문제 개수를 저장할 맵

        for (Quiz quiz : quizzes) {
            // 활성화된 세션 코드 조회
            quizSessionRepository.findByQuizAndIsActiveTrue(quiz).ifPresent(session -> {
                activeSessionCodes.put(quiz.getId(), session.getSessionCode());
                activeSessionIds.put(quiz.getId(), session.getId());
            });
            // 해당 퀴즈의 문제 개수 조회
            long count = questionRepository.countByQuiz(quiz); // 새로운 메서드 사용
            questionCounts.put(quiz.getId(), count);
        }
        model.addAttribute("activeSessionCodes", activeSessionCodes);
        model.addAttribute("activeSessionIds", activeSessionIds);
        model.addAttribute("questionCounts", questionCounts); // 문제 개수 맵 전달

        return "quiz_management";
    }

    // 특정 퀴즈의 문제 관리 페이지 (문제 목록 조회)
    @GetMapping("/teacher/quizzes/{quizId}/questions")
    public String questionManagement(@PathVariable("quizId") Long quizId, Model model) {
        // 해당 quizId를 가진 퀴즈를 찾습니다.
        Optional<Quiz> quizOptional = quizRepository.findById(quizId);
        if (quizOptional.isEmpty()) {
            // 퀴즈가 없으면 에러 페이지 또는 퀴즈 목록으로 리다이렉트
            return "redirect:/teacher/quizzes";
        }
        Quiz quiz = quizOptional.get();

        List<Question> questions = questionRepository.findByQuiz(quiz); // 해당 퀴즈의 모든 문제를 가져옵니다.
        model.addAttribute("quiz", quiz); // HTML로 퀴즈 정보 전달
        model.addAttribute("questions", questions); // HTML로 문제 목록 전달
        
        return "question_management"; // question_management.html을 보여줍니다.
    }

    // 문제 생성 처리
    @PostMapping("/teacher/quizzes/{quizId}/questions/create")
    public String createQuestion(@PathVariable Long quizId,
                                 @ModelAttribute Question question) {
        Optional<Quiz> quizOptional = quizRepository.findById(quizId);
        if (quizOptional.isEmpty()) {
            return "redirect:/teacher/quizzes"; // 퀴즈가 없으면 리다이렉트
        }
        Quiz quiz = quizOptional.get();
        question.setQuiz(quiz); // 생성할 문제에 해당 퀴즈를 연결합니다.
        questionRepository.save(question); // 문제를 데이터베이스에 저장합니다.
        return "redirect:/teacher/quizzes/" + quizId + "/questions"; // 문제 목록 페이지로 리다이렉트
    }

    // 문제 수정 처리
    @PostMapping("/teacher/quizzes/{quizId}/questions/update/{questionId}")
    public String updateQuestion(@PathVariable Long quizId,
                                 @PathVariable Long questionId,
                                 @ModelAttribute Question question) {
        Question existingQuestion = questionRepository.findById(questionId)
                .orElseThrow(() -> new IllegalArgumentException("Invalid question Id:" + questionId));

        existingQuestion.setContent(question.getContent());
        existingQuestion.setOption1(question.getOption1());
        existingQuestion.setOption2(question.getOption2());
        existingQuestion.setOption3(question.getOption3());
        existingQuestion.setOption4(question.getOption4());
        existingQuestion.setCorrectAnswer(question.getCorrectAnswer());
        // isActive는 별도의 토글 버튼으로 처리합니다.

        questionRepository.save(existingQuestion);
        return "redirect:/teacher/quizzes/" + quizId + "/questions";
    }

    // 문제 삭제 처리
    @PostMapping("/teacher/quizzes/{quizId}/questions/delete/{questionId}")
    public String deleteQuestion(@PathVariable Long quizId, @PathVariable Long questionId) {
        questionRepository.deleteById(questionId);
        return "redirect:/teacher/quizzes/" + quizId + "/questions";
    }

    // 문제 활성화/비활성화 토글
    @PostMapping("/teacher/quizzes/{quizId}/questions/toggleActive/{questionId}")
    public String toggleQuestionActive(@PathVariable Long quizId, @PathVariable Long questionId) {
        Question question = questionRepository.findById(questionId)
                .orElseThrow(() -> new IllegalArgumentException("Invalid question Id:" + questionId));
        question.setActive(!question.isActive()); // 현재 상태를 반전시킵니다.
        questionRepository.save(question);
        return "redirect:/teacher/quizzes/" + quizId + "/questions";
    }

    // 퀴즈 결과 메인 페이지 (퀴즈별 요약)
    @GetMapping("/teacher/quiz_results")
    public String quizResultsOverview(Model model) {
        List<Quiz> quizzes = quizRepository.findAll(); // 모든 퀴즈 목록을 가져옵니다.
        model.addAttribute("quizzes", quizzes);

        // 각 퀴즈별 평균 점수 및 응시 횟수 계산 (간단한 예시)
        Map<Long, Double> averageScores = new HashMap<>();
        Map<Long, Long> totalAttempts = new HashMap<>();

        for (Quiz quiz : quizzes) {
            List<QuizAttempt> attempts = quizAttemptRepository.findByQuiz(quiz);
            long count = attempts.size();
            double sumScore = attempts.stream().mapToInt(QuizAttempt::getScore).sum();

            totalAttempts.put(quiz.getId(), count);
            averageScores.put(quiz.getId(), count > 0 ? sumScore / count : 0.0);
        }
        model.addAttribute("totalAttempts", totalAttempts);
        model.addAttribute("averageScores", averageScores);

        return "quiz_results"; // quiz_results.html을 보여줍니다.
    }

    // 특정 퀴즈의 상세 응시 결과 (학생별 응답 내역)
    @GetMapping("/teacher/quiz_results/{quizId}/attempts")
    public String quizAttemptsDetail(@PathVariable Long quizId, Model model) {
        Optional<Quiz> quizOptional = quizRepository.findById(quizId);
        if (quizOptional.isEmpty()) {
            return "redirect:/teacher/quiz_results"; // 퀴즈가 없으면 리다이렉트
        }
        Quiz quiz = quizOptional.get();
        model.addAttribute("quiz", quiz);

        List<QuizAttempt> attempts = quizAttemptRepository.findByQuiz(quiz);
        model.addAttribute("attempts", attempts);

        return "quiz_results_detail"; // quiz_results_detail.html (새로운 상세 페이지)
    }

    // 특정 응시의 문제별 답변 상세 보기
    @GetMapping("/teacher/quiz_results/attempt/{attemptId}/answers")
    public String quizAttemptAnswers(@PathVariable Long attemptId, Model model) {
        Optional<QuizAttempt> attemptOptional = quizAttemptRepository.findById(attemptId);
        if (attemptOptional.isEmpty()) {
            return "redirect:/teacher/quiz_results"; // 응시 기록이 없으면 리다이렉트
        }
        QuizAttempt quizAttempt = attemptOptional.get();
        model.addAttribute("quizAttempt", quizAttempt);
        model.addAttribute("quiz", quizAttempt.getQuiz()); // 어떤 퀴즈인지도 전달

        List<QuizAnswer> answers = quizAnswerRepository.findByQuizAttempt(quizAttempt);
        model.addAttribute("answers", answers);

        return "quiz_results_answers"; // quiz_results_answers.html (또 다른 상세 페이지)
    }
}


