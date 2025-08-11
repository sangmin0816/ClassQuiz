package com.example.lcms.controller;

import com.example.lcms.entity.Quiz;
import com.example.lcms.entity.Question;
import com.example.lcms.entity.QuizAttempt;
import com.example.lcms.entity.QuizAnswer;
import com.example.lcms.entity.QuizSession;
import com.example.lcms.entity.User; // User 엔티티 추가

import com.example.lcms.repository.QuizRepository;
import com.example.lcms.repository.QuestionRepository;
import com.example.lcms.repository.QuizAttemptRepository;
import com.example.lcms.repository.QuizAnswerRepository;
import com.example.lcms.repository.QuizSessionRepository;
import com.example.lcms.repository.UserRepository; // UserRepository 추가

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.Map;
import java.util.HashMap;
import java.util.UUID;
import java.util.stream.Collectors; // Collectors import 추가

@Controller
@PreAuthorize("hasRole('TEACHER')")
public class TeacherController {

    private final QuizRepository quizRepository;
    private final QuestionRepository questionRepository;
    private final QuizAttemptRepository quizAttemptRepository;
    private final QuizAnswerRepository quizAnswerRepository;
    private final QuizSessionRepository quizSessionRepository;
    private final UserRepository userRepository; // UserRepository 주입

    // WebSocketController 의존성 주입 (선생님 시작/다음 버튼 호출은 기존 HTTP 컨트롤러에서 처리)
    private final WebSocketController webSocketController;


    public TeacherController(QuizRepository quizRepository,
                             QuestionRepository questionRepository,
                             QuizAttemptRepository quizAttemptRepository,
                             QuizAnswerRepository quizAnswerRepository,
                             QuizSessionRepository quizSessionRepository,
                             UserRepository userRepository, // UserRepository 추가
                             WebSocketController webSocketController) { // WebSocketController 추가
        this.quizRepository = quizRepository;
        this.questionRepository = questionRepository;
        this.quizAttemptRepository = quizAttemptRepository;
        this.quizAnswerRepository = quizAnswerRepository;
        this.quizSessionRepository = quizSessionRepository;
        this.userRepository = userRepository; // 주입
        this.webSocketController = webSocketController; // 주입
    }

    // 퀴즈 목록 조회
    @GetMapping("/teacher/quizzes")
    public String quizManagement(Model model) {
        List<Quiz> quizzes = quizRepository.findAll();
        Map<Long, Long> questionCounts = new HashMap<>();
        Map<Long, String> activeSessionCodes = new HashMap<>();
        Map<Long, Long> activeSessionIds = new HashMap<>();

        for (Quiz quiz : quizzes) {
            questionCounts.put(quiz.getId(), questionRepository.countByQuiz(quiz)); // Quiz 객체 전달
            Optional<QuizSession> activeSession = quizSessionRepository.findByQuizAndIsActiveTrue(quiz); // Quiz 객체 전달
            activeSession.ifPresent(session -> {
                activeSessionCodes.put(quiz.getId(), session.getSessionCode());
                activeSessionIds.put(quiz.getId(), session.getId());
            });
        }

        model.addAttribute("quizzes", quizzes);
        model.addAttribute("questionCounts", questionCounts);
        model.addAttribute("activeSessionCodes", activeSessionCodes);
        model.addAttribute("activeSessionIds", activeSessionIds);
        return "quiz_management";
    }

    // 새 퀴즈 생성
    @PostMapping("/teacher/quizzes/create")
    public String createQuiz(@ModelAttribute Quiz quiz, RedirectAttributes redirectAttributes) {
        quiz.setCreatedAt(LocalDateTime.now());
        quiz.setActive(true);
        quizRepository.save(quiz);
        redirectAttributes.addFlashAttribute("successMessage", "퀴즈 '" + quiz.getTitle() + "'가 성공적으로 생성되었습니다.");
        return "redirect:/teacher/quizzes";
    }

    // 퀴즈 수정
    @PostMapping("/teacher/quizzes/update/{id}")
    public String updateQuiz(@PathVariable Long id, @ModelAttribute Quiz updatedQuiz, RedirectAttributes redirectAttributes) {
        Optional<Quiz> quizOptional = quizRepository.findById(id);
        if (quizOptional.isPresent()) {
            Quiz quiz = quizOptional.get();
            quiz.setTitle(updatedQuiz.getTitle());
            quiz.setDescription(updatedQuiz.getDescription());
            quizRepository.save(quiz);
            redirectAttributes.addFlashAttribute("successMessage", "퀴즈 '" + quiz.getTitle() + "'가 성공적으로 업데이트되었습니다.");
        } else {
            redirectAttributes.addFlashAttribute("errorMessage", "퀴즈를 찾을 수 없습니다.");
        }
        return "redirect:/teacher/quizzes";
    }

    // 퀴즈 활성화/비활성화 토글
    @PostMapping("/teacher/quizzes/toggleActive/{id}")
    public String toggleQuizActive(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        Optional<Quiz> quizOptional = quizRepository.findById(id);
        if (quizOptional.isPresent()) {
            Quiz quiz = quizOptional.get();
            quiz.setActive(!quiz.isActive());
            quizRepository.save(quiz);
            redirectAttributes.addFlashAttribute("successMessage", "퀴즈 '" + quiz.getTitle() + "' 상태가 " + (quiz.isActive() ? "활성화" : "비활성화") + "되었습니다.");
        } else {
            redirectAttributes.addFlashAttribute("errorMessage", "퀴즈를 찾을 수 없습니다.");
        }
        return "redirect:/teacher/quizzes";
    }

    // 퀴즈 삭제
    @PostMapping("/teacher/quizzes/delete/{id}")
    public String deleteQuiz(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        Optional<Quiz> quizOptional = quizRepository.findById(id);
        if (quizOptional.isPresent()) {
            String quizTitle = quizOptional.get().getTitle();
            quizRepository.deleteById(id);
            redirectAttributes.addFlashAttribute("successMessage", "퀴즈 '" + quizTitle + "'가 성공적으로 삭제되었습니다.");
        } else {
            redirectAttributes.addFlashAttribute("errorMessage", "퀴즈를 찾을 수 없습니다.");
        }
        return "redirect:/teacher/quizzes";
    }

    // 퀴즈 세션 시작
    @PostMapping("/teacher/quizzes/{id}/startSession")
    public String startQuizSession(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        Optional<Quiz> quizOptional = quizRepository.findById(id);
        if (quizOptional.isEmpty()) {
            redirectAttributes.addFlashAttribute("errorMessage", "퀴즈를 찾을 수 없습니다.");
            return "redirect:/teacher/quizzes";
        }
        Quiz quiz = quizOptional.get();

        // 퀴즈에 활성화된 문제가 있는지 확인
        long activeQuestionCount = questionRepository.countByQuizAndIsActiveTrue(quiz); // Quiz 객체 전달
        if (activeQuestionCount == 0) {
            redirectAttributes.addFlashAttribute("errorMessage", "활성화된 문제가 없는 퀴즈는 시작할 수 없습니다.");
            return "redirect:/teacher/quizzes";
        }

        // 이미 활성화된 세션이 있는지 확인
        Optional<QuizSession> existingSession = quizSessionRepository.findByQuizAndIsActiveTrue(quiz); // Quiz 객체 전달
        if (existingSession.isPresent()) {
            redirectAttributes.addFlashAttribute("errorMessage", "이미 활성화된 퀴즈 세션이 있습니다. 코드: " + existingSession.get().getSessionCode());
            return "redirect:/teacher/quizzes";
        }

        String sessionCode = UUID.randomUUID().toString().substring(0, 6).toUpperCase();
        QuizSession quizSession = new QuizSession(quiz, sessionCode); // Quiz 객체 전달
        quizSessionRepository.save(quizSession);

        redirectAttributes.addFlashAttribute("successMessage", "퀴즈 '" + quiz.getTitle() + "' 세션이 시작되었습니다. 코드: " + sessionCode);
        redirectAttributes.addFlashAttribute("startedQuizId", quiz.getId()); // 시작된 퀴즈 ID 전달
        // 리다이렉트를 퀴즈 관리 페이지 대신 실시간 퀴즈 관리 페이지로 변경
        return "redirect:/teacher/quizzes/live/" + quizSession.getId();
    }

    // 퀴즈 세션 종료
    @PostMapping("/teacher/quizzes/{sessionId}/endSession")
    public String endQuizSession(@PathVariable Long sessionId, RedirectAttributes redirectAttributes) {
        Optional<QuizSession> sessionOptional = quizSessionRepository.findById(sessionId);
        if (sessionOptional.isEmpty()) {
            redirectAttributes.addFlashAttribute("errorMessage", "세션을 찾을 수 없습니다.");
            return "redirect:/teacher/quizzes";
        }
        QuizSession session = sessionOptional.get();

        // WebSocketController를 통해 퀴즈 세션 강제 종료 호출
        webSocketController.endQuiz(sessionId); // private 메서드를 public으로 변경하거나, 새 public 메서드 추가 필요
        // 현재는 직접 호출이 안되므로, 간접적으로 처리하거나 WebSocketController의 endQuiz를 public으로 변경해야 함.
        // 일단은 HTTP 요청으로 종료 로직을 유지하면서, 웹소켓 측에서도 종료하도록 처리할 예정

        redirectAttributes.addFlashAttribute("successMessage", "퀴즈 세션 '" + session.getSessionCode() + "'가 종료되었습니다.");
        return "redirect:/teacher/quizzes";
    }

    // 특정 퀴즈의 문제 관리 페이지 (문제 목록 조회)
    @GetMapping("/teacher/quizzes/{quizId}/questions")
    public String questionManagement(@PathVariable Long quizId, Model model, RedirectAttributes redirectAttributes) {
        Optional<Quiz> quizOptional = quizRepository.findById(quizId);
        if (quizOptional.isEmpty()) {
            redirectAttributes.addFlashAttribute("errorMessage", "존재하지 않는 퀴즈입니다. 퀴즈 ID: " + quizId);
            return "redirect:/teacher/quizzes";
        }
        Quiz quiz = quizOptional.get();

        List<Question> questions = questionRepository.findByQuiz(quiz);
        model.addAttribute("quiz", quiz);
        model.addAttribute("questions", questions);
        model.addAttribute("newQuestion", new Question());
        return "question_management";
    }

    // 새 문제 생성
    @PostMapping("/teacher/quizzes/{quizId}/questions/create")
    public String createQuestion(@PathVariable Long quizId, @ModelAttribute Question newQuestion, RedirectAttributes redirectAttributes) {
        Optional<Quiz> quizOptional = quizRepository.findById(quizId);
        if (quizOptional.isEmpty()) {
            redirectAttributes.addFlashAttribute("errorMessage", "문제를 추가할 퀴즈를 찾을 수 없습니다.");
            return "redirect:/teacher/quizzes";
        }
        Quiz quiz = quizOptional.get();
        newQuestion.setQuiz(quiz);
        newQuestion.setActive(true);
        questionRepository.save(newQuestion);
        redirectAttributes.addFlashAttribute("successMessage", "문제가 성공적으로 생성되었습니다.");
        return "redirect:/teacher/quizzes/" + quizId + "/questions";
    }

    // 문제 수정
    @PostMapping("/teacher/quizzes/{quizId}/questions/update/{questionId}")
    public String updateQuestion(@PathVariable Long quizId, @PathVariable Long questionId, @ModelAttribute Question updatedQuestion, RedirectAttributes redirectAttributes) {
        Optional<Question> questionOptional = questionRepository.findById(questionId);
        if (questionOptional.isPresent()) {
            Question question = questionOptional.get();
            question.setContent(updatedQuestion.getContent());
            question.setOption1(updatedQuestion.getOption1());
            question.setOption2(updatedQuestion.getOption2());
            question.setOption3(updatedQuestion.getOption3());
            question.setOption4(updatedQuestion.getOption4());
            question.setCorrectAnswer(updatedQuestion.getCorrectAnswer());
            questionRepository.save(question);
            redirectAttributes.addFlashAttribute("successMessage", "문제가 성공적으로 업데이트되었습니다.");
        } else {
            redirectAttributes.addFlashAttribute("errorMessage", "문제를 찾을 수 없습니다.");
        }
        return "redirect:/teacher/quizzes/" + quizId + "/questions";
    }

    // 문제 활성화/비활성화 토글
    @PostMapping("/teacher/quizzes/{quizId}/questions/toggleActive/{questionId}")
    public String toggleQuestionActive(@PathVariable Long quizId, @PathVariable Long questionId, RedirectAttributes redirectAttributes) {
        Optional<Question> questionOptional = questionRepository.findById(questionId);
        if (questionOptional.isPresent()) {
            Question question = questionOptional.get();
            question.setActive(!question.isActive());
            questionRepository.save(question);
            redirectAttributes.addFlashAttribute("successMessage", "문제 상태가 " + (question.isActive() ? "활성화" : "비활성화") + "되었습니다.");
        } else {
            redirectAttributes.addFlashAttribute("errorMessage", "문제를 찾을 수 없습니다.");
        }
        return "redirect:/teacher/quizzes/" + quizId + "/questions";
    }

    // 문제 삭제
    @PostMapping("/teacher/quizzes/{quizId}/questions/delete/{questionId}")
    public String deleteQuestion(@PathVariable Long quizId, @PathVariable Long questionId, RedirectAttributes redirectAttributes) {
        Optional<Question> questionOptional = questionRepository.findById(questionId);
        if (questionOptional.isPresent()) {
            questionRepository.deleteById(questionId);
            redirectAttributes.addFlashAttribute("successMessage", "문제가 성공적으로 삭제되었습니다.");
        } else {
            redirectAttributes.addFlashAttribute("errorMessage", "문제를 찾을 수 없습니다.");
        }
        return "redirect:/teacher/quizzes/" + quizId + "/questions";
    }

    // 퀴즈 결과 요약 페이지 (교사용)
    @GetMapping("/teacher/quiz_results")
    public String quizResultsOverview(Model model) {
        List<Quiz> quizzes = quizRepository.findAll();
        Map<Long, Long> totalAttempts = new HashMap<>();
        Map<Long, Double> averageScores = new HashMap<>();

        for (Quiz quiz : quizzes) {
            List<QuizAttempt> attempts = quizAttemptRepository.findByQuiz(quiz); // 퀴즈별 모든 응시 기록
            long count = attempts.size();
            double avgScore = attempts.stream()
                                    .mapToInt(QuizAttempt::getScore)
                                    .average()
                                    .orElse(0.0);
            totalAttempts.put(quiz.getId(), count);
            averageScores.put(quiz.getId(), avgScore);
        }

        model.addAttribute("quizzes", quizzes);
        model.addAttribute("totalAttempts", totalAttempts);
        model.addAttribute("averageScores", averageScores);
        return "quiz_results";
    }

    // 특정 퀴즈의 응시 기록 상세 (교사용)
    @GetMapping("/teacher/quiz_results/{quizId}/attempts")
    public String quizAttemptsDetail(@PathVariable Long quizId, Model model, RedirectAttributes redirectAttributes) {
        Optional<Quiz> quizOptional = quizRepository.findById(quizId);
        if (quizOptional.isEmpty()) {
            redirectAttributes.addFlashAttribute("errorMessage", "해당 퀴즈를 찾을 수 없습니다.");
            return "redirect:/teacher/quiz_results";
        }
        Quiz quiz = quizOptional.get();
        List<QuizAttempt> attempts = quizAttemptRepository.findByQuiz(quiz); // 퀴즈 객체로 조회
        
        model.addAttribute("quiz", quiz);
        model.addAttribute("attempts", attempts);
        return "quiz_results_detail";
    }

    // 특정 응시 기록의 답변 상세 (교사용)
    @GetMapping("/teacher/quiz_results/attempt/{attemptId}/answers")
    public String quizAttemptAnswers(@PathVariable Long attemptId, Model model, RedirectAttributes redirectAttributes) {
        Optional<QuizAttempt> attemptOptional = quizAttemptRepository.findById(attemptId);
        if (attemptOptional.isEmpty()) {
            redirectAttributes.addFlashAttribute("errorMessage", "해당 퀴즈 응시 기록을 찾을 수 없습니다.");
            return "redirect:/teacher/quiz_results"; // 적절한 리다이렉션으로 변경 가능
        }
        QuizAttempt quizAttempt = attemptOptional.get();
        Quiz quiz = quizAttempt.getQuiz(); // QuizAttempt에서 Quiz 객체 직접 참조

        List<QuizAnswer> answers = quizAnswerRepository.findByQuizAttempt(quizAttempt); // QuizAttempt 객체로 조회

        model.addAttribute("quiz", quiz);
        model.addAttribute("quizAttempt", quizAttempt);
        model.addAttribute("answers", answers);
        return "quiz_results_answers";
    }

    // ==========================================================
    // 실시간 퀴즈 관련 엔드포인트
    // ==========================================================

    @GetMapping("/teacher/quizzes/live/{sessionId}")
    public String liveQuizManagement(@PathVariable Long sessionId, Model model, RedirectAttributes redirectAttributes) {
        Optional<QuizSession> sessionOptional = quizSessionRepository.findById(sessionId);
        if (sessionOptional.isEmpty()) {
            redirectAttributes.addFlashAttribute("errorMessage", "실시간 퀴즈 세션을 찾을 수 없습니다.");
            return "redirect:/teacher/quizzes";
        }
        QuizSession quizSession = sessionOptional.get();
        Quiz quiz = quizSession.getQuiz(); // 퀴즈 세션에서 퀴즈 정보 가져오기

        model.addAttribute("quizSession", quizSession);
        model.addAttribute("quiz", quiz); // 퀴즈 제목 등을 화면에 표시하기 위함
        return "quiz_live_teacher"; // 새로운 HTML 템플릿 이름
    }
}
