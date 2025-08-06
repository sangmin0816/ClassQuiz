package com.example.lcms.controller;

import com.example.lcms.entity.QuizSession;
import com.example.lcms.entity.QuizAttempt;
import com.example.lcms.entity.QuizAnswer;
import com.example.lcms.entity.User;
import com.example.lcms.entity.Quiz;
import com.example.lcms.entity.Question;

import com.example.lcms.repository.QuizSessionRepository;
import com.example.lcms.repository.QuizAttemptRepository;
import com.example.lcms.repository.QuizAnswerRepository;
import com.example.lcms.repository.UserRepository;
import com.example.lcms.repository.QuestionRepository;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import jakarta.servlet.http.HttpSession;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.ArrayList;
import java.util.Optional;
import java.util.concurrent.ThreadLocalRandom;

@Controller
@PreAuthorize("hasRole('STUDENT')")
public class StudentController {

    private final QuizSessionRepository quizSessionRepository;
    private final QuizAttemptRepository quizAttemptRepository;
    private final QuizAnswerRepository quizAnswerRepository;
    private final UserRepository userRepository;
    private final QuestionRepository questionRepository;

    public StudentController(QuizSessionRepository quizSessionRepository,
                             QuizAttemptRepository quizAttemptRepository,
                             QuizAnswerRepository quizAnswerRepository,
                             UserRepository userRepository,
                             QuestionRepository questionRepository) {
        this.quizSessionRepository = quizSessionRepository;
        this.quizAttemptRepository = quizAttemptRepository;
        this.quizAnswerRepository = quizAnswerRepository;
        this.userRepository = userRepository;
        this.questionRepository = questionRepository;
    }

    @GetMapping("/student/dashboard")
    public String studentDashboard(Model model) {
        return "student_dashboard";
    }

    @PostMapping("/student/quiz_entry")
    public String processQuizEntry(@RequestParam String sessionCode, RedirectAttributes redirectAttributes, HttpSession httpSession) {
        Optional<QuizSession> sessionOptional = quizSessionRepository.findBySessionCode(sessionCode.toUpperCase());
        
        if (sessionOptional.isEmpty() || !sessionOptional.get().isActive()) {
            redirectAttributes.addFlashAttribute("errorMessage", "유효하지 않거나 종료된 퀴즈 코드입니다.");
            return "redirect:/student/dashboard";
        }

        QuizSession quizSession = sessionOptional.get();
        Quiz quiz = quizSession.getQuiz();

        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String username = authentication.getName();
        Optional<User> userOptional = userRepository.findByUsername(username);
        if (userOptional.isEmpty()) {
            redirectAttributes.addFlashAttribute("errorMessage", "사용자 정보를 찾을 수 없습니다. 다시 로그인해주세요.");
            return "redirect:/login";
        }
        User currentUser = userOptional.get();

        Optional<QuizAttempt> existingAttempt = quizAttemptRepository.findByUserAndQuizSession(currentUser, quizSession);
        if (existingAttempt.isPresent()) {
            redirectAttributes.addFlashAttribute("errorMessage", "이미 이 퀴즈 세션에 참여했습니다.");
            return "redirect:/student/dashboard";
        }

        List<Question> questions = questionRepository.findByQuizAndIsActiveTrue(quiz);
        if (questions.isEmpty()) {
            redirectAttributes.addFlashAttribute("errorMessage", "이 퀴즈에는 활성화된 문제가 없습니다.");
            return "redirect:/student/dashboard";
        }

        QuizAttempt newAttempt = new QuizAttempt(currentUser, quiz, quizSession, questions.size());
        quizAttemptRepository.save(newAttempt);

        // 세션에 저장할 때 타입 안전성을 고려하여 List<Question>으로 명시
        httpSession.setAttribute("currentQuizAttemptId", newAttempt.getId());
        httpSession.setAttribute("currentQuestionIndex", 0);
        httpSession.setAttribute("quizQuestions", questions); // List<Question> 그대로 저장
        httpSession.setAttribute("questionStartTime", System.currentTimeMillis());

        return "redirect:/student/quiz/play/" + newAttempt.getId();
    }

    @GetMapping("/student/quiz/play/{attemptId}")
    @SuppressWarnings("unchecked") // HttpSession에서 List<Question> 캐스팅 경고 억제
    public String playQuiz(@PathVariable Long attemptId, Model model, HttpSession httpSession, RedirectAttributes redirectAttributes) {
        // 세션에서 가져올 때 타입 안전성 확보
        Long currentQuizAttemptId = (Long) httpSession.getAttribute("currentQuizAttemptId");
        Integer currentQuestionIndex = (Integer) httpSession.getAttribute("currentQuestionIndex");
        
        // List<Question> 캐스팅 시 경고를 피하기 위해 명시적 타입 체크
        List<Question> questions = null;
        Object questionsObj = httpSession.getAttribute("quizQuestions");
        if (questionsObj instanceof List) {
            questions = (List<Question>) questionsObj;
        }

        if (currentQuizAttemptId == null || !currentQuizAttemptId.equals(attemptId) || currentQuestionIndex == null || questions == null || questions.isEmpty()) {
            redirectAttributes.addFlashAttribute("errorMessage", "유효하지 않은 퀴즈 세션입니다. 다시 참여해주세요.");
            return "redirect:/student/dashboard";
        }

        if (currentQuestionIndex >= questions.size()) {
            return "redirect:/student/quiz/finish/" + attemptId;
        }

        Question currentQuestion = questions.get(currentQuestionIndex);

        List<String> options = new ArrayList<>();
        options.add(currentQuestion.getOption1());
        options.add(currentQuestion.getOption2());
        options.add(currentQuestion.getOption3());
        options.add(currentQuestion.getOption4());
        Collections.shuffle(options, ThreadLocalRandom.current());

        model.addAttribute("quizAttemptId", attemptId);
        model.addAttribute("questionNumber", currentQuestionIndex + 1);
        model.addAttribute("totalQuestions", questions.size());
        model.addAttribute("question", currentQuestion);
        model.addAttribute("options", options);

        httpSession.setAttribute("questionStartTime", System.currentTimeMillis());

        return "quiz_play";
    }

    @PostMapping("/student/quiz/submitAnswer")
    @SuppressWarnings("unchecked") // HttpSession에서 List<Question> 캐스팅 경고 억제
    public String submitAnswer(@RequestParam Long quizAttemptId,
                               @RequestParam Long questionId,
                               @RequestParam String selectedAnswerContent,
                               HttpSession httpSession,
                               RedirectAttributes redirectAttributes) {

        Long currentQuizAttemptId = (Long) httpSession.getAttribute("currentQuizAttemptId");
        Integer currentQuestionIndex = (Integer) httpSession.getAttribute("currentQuestionIndex");
        
        // List<Question> 캐스팅 시 경고를 피하기 위해 명시적 타입 체크
        List<Question> questions = null;
        Object questionsObj = httpSession.getAttribute("quizQuestions");
        if (questionsObj instanceof List) {
            questions = (List<Question>) questionsObj;
        }
        
        Long questionStartTime = (Long) httpSession.getAttribute("questionStartTime");

        if (currentQuizAttemptId == null || !currentQuizAttemptId.equals(quizAttemptId) || currentQuestionIndex == null || questions == null || questions.isEmpty() || questionStartTime == null) {
            redirectAttributes.addFlashAttribute("errorMessage", "유효하지 않은 퀴즈 세션입니다. 다시 참여해주세요.");
            return "redirect:/student/dashboard";
        }

        if (!questions.get(currentQuestionIndex).getId().equals(questionId)) {
            redirectAttributes.addFlashAttribute("errorMessage", "잘못된 문제에 대한 답변입니다.");
            return "redirect:/student/quiz/play/" + quizAttemptId;
        }

        Question currentQuestion = questions.get(currentQuestionIndex);
        long responseTimeMillis = System.currentTimeMillis() - questionStartTime;

        boolean isCorrect = selectedAnswerContent.equals(currentQuestion.getCorrectAnswer());

        QuizAttempt quizAttempt = quizAttemptRepository.findById(quizAttemptId).orElse(null);
        if (quizAttempt == null) {
            redirectAttributes.addFlashAttribute("errorMessage", "유효하지 않은 퀴즈 응시입니다.");
            return "redirect:/student/dashboard";
        }

        QuizAnswer quizAnswer = new QuizAnswer(quizAttempt, currentQuestion, selectedAnswerContent, isCorrect, responseTimeMillis);
        quizAnswerRepository.save(quizAnswer);

        if (isCorrect) {
            quizAttempt.setCorrectAnswers(quizAttempt.getCorrectAnswers() + 1);
            quizAttempt.setScore((int) (((double)(quizAttempt.getCorrectAnswers()) / quizAttempt.getTotalQuestions()) * 100));
        }
        quizAttemptRepository.save(quizAttempt);

        httpSession.setAttribute("currentQuestionIndex", currentQuestionIndex + 1);

        if (currentQuestionIndex + 1 >= questions.size()) {
            quizAttempt.setEndTime(LocalDateTime.now());
            quizAttemptRepository.save(quizAttempt);

            httpSession.removeAttribute("currentQuizAttemptId");
            httpSession.removeAttribute("currentQuestionIndex");
            httpSession.removeAttribute("quizQuestions");
            httpSession.removeAttribute("questionStartTime");

            return "redirect:/student/quiz/finish/" + quizAttemptId;
        } else {
            return "redirect:/student/quiz/play/" + quizAttemptId;
        }
    }

    @GetMapping("/student/quiz/finish/{attemptId}")
    public String finishQuiz(@PathVariable Long attemptId, Model model, RedirectAttributes redirectAttributes) {
        Optional<QuizAttempt> quizAttemptOptional = quizAttemptRepository.findById(attemptId);
        if (quizAttemptOptional.isEmpty()) {
            redirectAttributes.addFlashAttribute("errorMessage", "유효하지 않은 퀴즈 응시 기록입니다.");
            return "redirect:/student/dashboard";
        }
        QuizAttempt quizAttempt = quizAttemptOptional.get();

        model.addAttribute("quizAttempt", quizAttempt);
        return "quiz_finish";
    }

    @GetMapping("/student/my_quiz_results")
    public String myQuizResults(Model model) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String username = authentication.getName();

        Optional<User> userOptional = userRepository.findByUsername(username);
        if (userOptional.isEmpty()) {
            return "redirect:/login";
        }
        User currentUser = userOptional.get();

        List<QuizAttempt> myAttempts = quizAttemptRepository.findByUser(currentUser);
        model.addAttribute("myAttempts", myAttempts);

        return "student_quiz_results";
    }
}
