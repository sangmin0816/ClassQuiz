package com.example.lcms.controller;

import com.example.lcms.entity.Quiz;
import com.example.lcms.entity.Question;
import com.example.lcms.entity.QuizAttempt;
import com.example.lcms.entity.QuizAnswer;
import com.example.lcms.entity.QuizSession;
import com.example.lcms.entity.User;

import com.example.lcms.repository.QuizRepository;
import com.example.lcms.repository.QuestionRepository;
import com.example.lcms.repository.QuizAttemptRepository;
import com.example.lcms.repository.QuizAnswerRepository;
import com.example.lcms.repository.QuizSessionRepository;
import com.example.lcms.repository.UserRepository;
import com.example.lcms.config.CustomUserDetails;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.Collections; // Collections import 추가
import java.util.concurrent.ThreadLocalRandom; // ThreadLocalRandom import 추가


@Controller
@PreAuthorize("hasRole('STUDENT')")
public class StudentController {

    private final QuizRepository quizRepository;
    private final QuestionRepository questionRepository;
    private final QuizAttemptRepository quizAttemptRepository;
    private final QuizAnswerRepository quizAnswerRepository;
    private final QuizSessionRepository quizSessionRepository;
    private final UserRepository userRepository;

    public StudentController(QuizRepository quizRepository,
                             QuestionRepository questionRepository,
                             QuizAttemptRepository quizAttemptRepository,
                             QuizAnswerRepository quizAnswerRepository,
                             QuizSessionRepository quizSessionRepository,
                             UserRepository userRepository) {
        this.quizRepository = quizRepository;
        this.questionRepository = questionRepository;
        this.quizAttemptRepository = quizAttemptRepository;
        this.quizAnswerRepository = quizAnswerRepository;
        this.quizSessionRepository = quizSessionRepository;
        this.userRepository = userRepository;
    }

    @GetMapping("/student/dashboard")
    public String studentDashboard(Model model) {
        return "student_dashboard";
    }

    // 학생의 지난 퀴즈 결과 조회 페이지
    @GetMapping("/student/my_quiz_results")
    public String myQuizResults(Model model, Authentication authentication) {
        if (authentication == null || !(authentication.getPrincipal() instanceof CustomUserDetails)) {
            return "redirect:/login"; // 로그인 페이지로 리다이렉트
        }
        CustomUserDetails userDetails = (CustomUserDetails) authentication.getPrincipal();
        Optional<User> userOptional = userRepository.findByUsername(userDetails.getUsername());

        if (userOptional.isPresent()) {
            User currentUser = userOptional.get();
            // 현재 로그인한 학생의 모든 퀴즈 응시 기록 조회
            List<QuizAttempt> myAttempts = quizAttemptRepository.findByUser(currentUser);
            model.addAttribute("myAttempts", myAttempts);
        } else {
            // 사용자 정보가 없으면 에러 메시지
            model.addAttribute("errorMessage", "사용자 정보를 찾을 수 없습니다.");
            model.addAttribute("myAttempts", Collections.emptyList()); // 빈 리스트 전달
        }
        return "student_quiz_results";
    }


    // 퀴즈 참여 코드 입력 페이지 (기존과 동일)
    @GetMapping("/student/quiz_entry")
    public String quizEntryForm() {
        return "quiz_entry";
    }

    // 퀴즈 참여 코드 제출 (실시간 퀴즈 세션 연결)
    @PostMapping("/student/quiz_entry")
    public String processQuizEntry(@RequestParam String sessionCode,
                                   Authentication authentication,
                                   RedirectAttributes redirectAttributes) {
        Optional<QuizSession> sessionOptional = quizSessionRepository.findBySessionCodeAndIsActiveTrue(sessionCode);

        if (sessionOptional.isEmpty()) {
            redirectAttributes.addFlashAttribute("errorMessage", "유효하지 않거나 활성화되지 않은 퀴즈 코드입니다.");
            return "redirect:/student/quiz_entry";
        }

        QuizSession quizSession = sessionOptional.get();
        User currentUser = ((CustomUserDetails) authentication.getPrincipal()).getUser();

        // 이미 참여 중인(진행 중인) 퀴즈가 있는지 확인
        Optional<QuizAttempt> existingAttempt = quizAttemptRepository.findByUserAndQuizSessionAndEndTimeIsNull(currentUser, quizSession);
        if (existingAttempt.isPresent()) {
            // 이미 진행 중인 퀴즈가 있다면 해당 라이브 페이지로 리다이렉트
            redirectAttributes.addFlashAttribute("errorMessage", "이미 참여 중인 퀴즈 세션입니다.");
            return "redirect:/student/quiz_live/" + quizSession.getId() + "?attemptId=" + existingAttempt.get().getId();
        }

        // 새 퀴즈 응시 (QuizAttempt) 생성
        QuizAttempt quizAttempt = new QuizAttempt();
        quizAttempt.setUser(currentUser);
        quizAttempt.setQuiz(quizSession.getQuiz()); // 퀴즈 세션에서 퀴즈 가져오기
        quizAttempt.setQuizSession(quizSession);
        quizAttempt.setStartTime(LocalDateTime.now());
        quizAttempt.setScore(0);
        quizAttempt.setCorrectAnswers(0);
        
        // 해당 퀴즈의 활성화된 문제 총 개수를 가져와서 totalQuestions 설정
        long totalQuestionsCount = questionRepository.countByQuizAndIsActiveTrue(quizSession.getQuiz());
        quizAttempt.setTotalQuestions((int) totalQuestionsCount); // Long을 int로 캐스팅

        quizAttemptRepository.save(quizAttempt);

        redirectAttributes.addFlashAttribute("successMessage", "퀴즈 세션에 참여합니다.");
        // 실시간 퀴즈 참여 페이지로 리다이렉트
        return "redirect:/student/quiz_live/" + quizSession.getId() + "?attemptId=" + quizAttempt.getId();
    }

    // 실시간 퀴즈 참여 페이지
    @GetMapping("/student/quiz_live/{sessionId}")
    public String liveQuizStudent(@PathVariable Long sessionId,
                                  @RequestParam(required = false) Long attemptId, // URL 쿼리 파라미터로 attemptId 받기
                                  Model model,
                                  Authentication authentication,
                                  RedirectAttributes redirectAttributes) {
        
        Optional<QuizSession> sessionOptional = quizSessionRepository.findById(sessionId);
        if (sessionOptional.isEmpty() || !sessionOptional.get().isActive()) {
            redirectAttributes.addFlashAttribute("errorMessage", "활성화되지 않았거나 존재하지 않는 퀴즈 세션입니다.");
            return "redirect:/student/dashboard";
        }
        QuizSession quizSession = sessionOptional.get();

        User currentUser = ((CustomUserDetails) authentication.getPrincipal()).getUser();
        QuizAttempt currentAttempt = null;

        if (attemptId != null) {
            Optional<QuizAttempt> attemptOptional = quizAttemptRepository.findById(attemptId);
            if (attemptOptional.isPresent() && attemptOptional.get().getUser().equals(currentUser) && attemptOptional.get().getQuizSession().equals(quizSession)) {
                currentAttempt = attemptOptional.get();
                // 이미 종료된 시도라면 대시보드로 리다이렉트
                if (currentAttempt.getEndTime() != null) {
                    redirectAttributes.addFlashAttribute("errorMessage", "이미 종료된 퀴즈 시도입니다.");
                    return "redirect:/student/dashboard";
                }
            }
        }

        // attemptId가 없거나 유효하지 않으면 새로 생성 (혹은 이미 진행 중인 세션 조회)
        if (currentAttempt == null) {
            Optional<QuizAttempt> existingAttempt = quizAttemptRepository.findByUserAndQuizSessionAndEndTimeIsNull(currentUser, quizSession);
            if (existingAttempt.isPresent()) {
                currentAttempt = existingAttempt.get();
            } else {
                // 이 상황은 /student/quiz_entry를 거치지 않고 직접 접근했거나, 세션이 만료된 경우이므로 새로 시작하도록 유도
                redirectAttributes.addFlashAttribute("errorMessage", "유효한 퀴즈 응시 기록이 없거나 세션이 만료되었습니다. 다시 참여 코드를 입력해주세요.");
                return "redirect:/student/quiz_entry";
            }
        }
        
        model.addAttribute("quizSession", quizSession);
        model.addAttribute("quizAttempt", currentAttempt);
        model.addAttribute("quiz", quizSession.getQuiz()); // 퀴즈 제목 등을 표시하기 위함

        return "quiz_live_student"; // 새로운 HTML 템플릿 이름
    }

    // 기존 퀴즈 풀이 로직 (비실시간)은 필요에 따라 유지하거나 삭제 가능
    // 현재는 실시간 퀴즈가 메인 흐름이므로 이 메서드는 사용되지 않을 수 있음
    @GetMapping("/student/quiz_play/{quizId}")
    public String playQuiz(@PathVariable Long quizId, Model model, Authentication authentication, RedirectAttributes redirectAttributes) {
        Optional<Quiz> quizOptional = quizRepository.findById(quizId);
        if (quizOptional.isEmpty() || !quizOptional.get().isActive()) {
            redirectAttributes.addFlashAttribute("errorMessage", "활성화되지 않았거나 존재하지 않는 퀴즈입니다.");
            return "redirect:/student/dashboard";
        }
        Quiz quiz = quizOptional.get();
        User currentUser = ((CustomUserDetails) authentication.getPrincipal()).getUser();

        List<Question> questions = questionRepository.findByQuizAndIsActiveTrue(quiz);
        if (questions.isEmpty()) {
            redirectAttributes.addFlashAttribute("errorMessage", "이 퀴즈에는 활성화된 문제가 없습니다.");
            return "redirect:/student/dashboard";
        }

        // 기존 비실시간 퀴즈 시도에 대한 로직
        Optional<QuizAttempt> existingAttempt = quizAttemptRepository.findByUserAndQuizAndEndTimeIsNull(currentUser, quiz);
        if (existingAttempt.isPresent()) {
            redirectAttributes.addFlashAttribute("errorMessage", "이미 해당 퀴즈를 진행 중입니다. 이어서 풀어주세요.");
            // 특정 시도에 대한 현재 문제로 리다이렉트하거나 오류 처리
            return "redirect:/student/dashboard"; // 임시로 대시보드로 리다이렉트
        }

        QuizAttempt quizAttempt = new QuizAttempt();
        quizAttempt.setUser(currentUser);
        quizAttempt.setQuiz(quiz);
        quizAttempt.setStartTime(LocalDateTime.now());
        quizAttempt.setScore(0);
        quizAttempt.setCorrectAnswers(0);
        quizAttempt.setTotalQuestions(questions.size());
        quizAttemptRepository.save(quizAttempt);

        // 첫 번째 문제만 전달
        Question firstQuestion = questions.get(0);
        List<String> options = new java.util.ArrayList<>();
        options.add(firstQuestion.getOption1());
        options.add(firstQuestion.getOption2());
        options.add(firstQuestion.getOption3());
        options.add(firstQuestion.getOption4());
        Collections.shuffle(options, ThreadLocalRandom.current()); // 보기 무작위로 섞기

        model.addAttribute("quiz", quiz);
        model.addAttribute("question", firstQuestion);
        model.addAttribute("options", options);
        model.addAttribute("currentQuestionNumber", 1);
        model.addAttribute("totalQuestions", questions.size());
        model.addAttribute("quizAttemptId", quizAttempt.getId());
        model.addAttribute("startTime", System.currentTimeMillis()); // 응답 시간 계산을 위한 시작 시간

        return "quiz_play";
    }

    // 기존 퀴즈 답변 제출 로직 (비실시간)은 현재 실시간 퀴즈 흐름에서 사용되지 않음
    // 실시간 퀴즈에서는 웹소켓을 통해 WebSocketController의 submitAnswer가 호출됨
    @PostMapping("/student/quiz/submitAnswer")
    public String submitAnswer(@RequestParam Long questionId,
                               @RequestParam Long quizAttemptId,
                               @RequestParam String selectedAnswerContent,
                               @RequestParam Long startTime, // 문제 시작 시간 (밀리초)
                               RedirectAttributes redirectAttributes,
                               Model model) {
        
        Optional<Question> questionOptional = questionRepository.findById(questionId);
        Optional<QuizAttempt> attemptOptional = quizAttemptRepository.findById(quizAttemptId);

        if (questionOptional.isEmpty() || attemptOptional.isEmpty()) {
            redirectAttributes.addFlashAttribute("errorMessage", "유효하지 않은 문제 또는 응시 정보입니다.");
            return "redirect:/student/dashboard";
        }

        Question question = questionOptional.get();
        QuizAttempt quizAttempt = attemptOptional.get();

        // 중복 답변 방지: 이미 이 문제에 답변했는지 확인
        boolean alreadyAnswered = quizAnswerRepository.findByQuizAttempt(quizAttempt).stream()
            .anyMatch(qa -> qa.getQuestion().getId().equals(questionId));

        if (alreadyAnswered) {
            redirectAttributes.addFlashAttribute("errorMessage", "이미 답변한 문제입니다.");
            return "redirect:/student/dashboard"; // 또는 다음 문제로 리다이렉트
        }

        boolean isCorrect = selectedAnswerContent.equals(question.getCorrectAnswer());
        long responseTimeMillis = System.currentTimeMillis() - startTime;

        QuizAnswer quizAnswer = new QuizAnswer(quizAttempt, question, selectedAnswerContent, isCorrect, responseTimeMillis);
        quizAnswerRepository.save(quizAnswer);

        if (isCorrect) {
            quizAttempt.setCorrectAnswers(quizAttempt.getCorrectAnswers() + 1);
        }
        // 점수 계산을 모든 답변이 완료된 후에 하는 대신, 각 답변 제출 시 업데이트
        // 총 점수는 (맞은 개수 / 총 문제 수) * 100
        quizAttempt.setScore((int) Math.round(((double) quizAttempt.getCorrectAnswers() / quizAttempt.getTotalQuestions()) * 100));
        quizAttemptRepository.save(quizAttempt);


        // 다음 문제로 이동 또는 퀴즈 종료
        List<Question> allQuestions = questionRepository.findByQuizAndIsActiveTrue(quizAttempt.getQuiz()); // Quiz 객체 전달
        int nextQuestionIndex = -1;

        // 현재 응시된 답변의 개수를 기준으로 다음 문제 인덱스 계산
        long answeredCount = quizAnswerRepository.countByQuizAttempt(quizAttempt);

        if (answeredCount < allQuestions.size()) {
            nextQuestionIndex = (int) answeredCount; // 다음 문제는 현재 답변된 개수와 동일한 인덱스
        }
        
        if (nextQuestionIndex != -1 && nextQuestionIndex < allQuestions.size()) {
            Question nextQuestion = allQuestions.get(nextQuestionIndex);
            List<String> options = new java.util.ArrayList<>();
            options.add(nextQuestion.getOption1());
            options.add(nextQuestion.getOption2());
            options.add(nextQuestion.getOption3());
            options.add(nextQuestion.getOption4());
            Collections.shuffle(options, ThreadLocalRandom.current()); // 보기 무작위로 섞기

            model.addAttribute("quiz", quizAttempt.getQuiz());
            model.addAttribute("question", nextQuestion);
            model.addAttribute("options", options);
            model.addAttribute("currentQuestionNumber", nextQuestionIndex + 1);
            model.addAttribute("totalQuestions", allQuestions.size());
            model.addAttribute("quizAttemptId", quizAttempt.getId());
            model.addAttribute("startTime", System.currentTimeMillis()); // 응답 시간 계산을 위한 시작 시간

            return "quiz_play";
        } else {
            // 퀴즈 완료
            quizAttempt.setEndTime(LocalDateTime.now());
            quizAttemptRepository.save(quizAttempt);
            model.addAttribute("quizAttempt", quizAttempt);
            return "quiz_finish";
        }
    }

    @GetMapping("/student/quiz_finish")
    public String quizFinish(@RequestParam Long attemptId, Model model, RedirectAttributes redirectAttributes) {
        Optional<QuizAttempt> attemptOptional = quizAttemptRepository.findById(attemptId);
        if (attemptOptional.isEmpty()) {
            redirectAttributes.addFlashAttribute("errorMessage", "유효하지 않은 퀴즈 응시 기록입니다.");
            return "redirect:/student/dashboard";
        }
        model.addAttribute("quizAttempt", attemptOptional.get());
        return "quiz_finish";
    }
}
