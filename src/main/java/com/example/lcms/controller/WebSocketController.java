package com.example.lcms.controller; // 패키지 변경됨

import com.example.lcms.entity.Question;
import com.example.lcms.entity.QuizAttempt;
import com.example.lcms.entity.QuizAnswer;
import com.example.lcms.entity.QuizSession;
import com.example.lcms.entity.Quiz;
import com.example.lcms.entity.User;
import com.example.lcms.repository.QuestionRepository;
import com.example.lcms.repository.QuizAttemptRepository;
import com.example.lcms.repository.QuizAnswerRepository;
import com.example.lcms.repository.QuizSessionRepository;
import com.example.lcms.repository.QuizRepository;
import com.example.lcms.repository.UserRepository;
import com.example.lcms.config.CustomUserDetails;
import com.example.lcms.websocket.AnswerMessage; // websocket 패키지 명시
import com.example.lcms.websocket.QuestionMessage; // websocket 패키지 명시
import com.example.lcms.websocket.QuizMessage; // websocket 패키지 명시 (현재 사용 안함)
import com.example.lcms.websocket.QuizStatusMessage; // websocket 패키지 명시


import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.SimpMessageHeaderAccessor;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.ArrayList;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.ThreadLocalRandom;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


@Controller
public class WebSocketController {

    private static final Logger logger = LoggerFactory.getLogger(WebSocketController.class);

    private final SimpMessagingTemplate messagingTemplate;
    private final QuizSessionRepository quizSessionRepository;
    private final QuestionRepository questionRepository;
    private final QuizAttemptRepository quizAttemptRepository;
    private final QuizAnswerRepository quizAnswerRepository;
    private final QuizRepository quizRepository;
    private final UserRepository userRepository;

    private final Map<Long, QuizSessionState> quizSessionStates = new ConcurrentHashMap<>();

    private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(10);

    public WebSocketController(SimpMessagingTemplate messagingTemplate,
                               QuizSessionRepository quizSessionRepository,
                               QuestionRepository questionRepository,
                               QuizAttemptRepository quizAttemptRepository,
                               QuizAnswerRepository quizAnswerRepository,
                               QuizRepository quizRepository,
                               UserRepository userRepository) {
        this.messagingTemplate = messagingTemplate;
        this.quizSessionRepository = quizSessionRepository;
        this.questionRepository = questionRepository;
        this.quizAttemptRepository = quizAttemptRepository;
        this.quizAnswerRepository = quizAnswerRepository;
        this.quizRepository = quizRepository;
        this.userRepository = userRepository;
    }

    @MessageMapping("/quiz.start/{sessionId}")
    public void startQuiz(@DestinationVariable Long sessionId, SimpMessageHeaderAccessor headerAccessor) {
        Authentication authentication = (Authentication) headerAccessor.getUser();
        if (authentication == null || !authentication.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals("ROLE_TEACHER"))) {
            logger.warn("Unauthorized attempt to start quiz session {}. User: {}", sessionId, authentication != null ? authentication.getName() : "Anonymous");
            return;
        }

        Optional<QuizSession> sessionOptional = quizSessionRepository.findById(sessionId);
        if (sessionOptional.isEmpty() || !sessionOptional.get().isActive()) {
            logger.warn("Attempted to start inactive or non-existent quiz session {}.", sessionId);
            return;
        }
        QuizSession quizSession = sessionOptional.get();
        Quiz quiz = quizSession.getQuiz();

        List<Question> questions = questionRepository.findByQuizAndIsActiveTrue(quiz);
        if (questions.isEmpty()) {
            logger.warn("Quiz session {} has no active questions. Cannot start.", sessionId);
            return;
        }

        QuizSessionState state = new QuizSessionState(quizSession, questions);
        quizSessionStates.put(sessionId, state);
        
        logger.info("Starting quiz session {}. First question for quiz {} will be sent.", sessionId, quiz.getTitle());
        sendNextQuestion(sessionId);
    }

    @MessageMapping("/quiz.nextQuestion/{sessionId}")
    public void nextQuestion(@DestinationVariable Long sessionId, SimpMessageHeaderAccessor headerAccessor) {
        Authentication authentication = (Authentication) headerAccessor.getUser();
        if (authentication == null || !authentication.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals("ROLE_TEACHER"))) {
            logger.warn("Unauthorized attempt to advance quiz session {}. User: {}", sessionId, authentication != null ? authentication.getName() : "Anonymous");
            return;
        }

        QuizSessionState state = quizSessionStates.get(sessionId);
        if (state == null || state.isEnded()) {
            logger.warn("Attempted to advance inactive or ended quiz session {}.", sessionId);
            return;
        }
        
        logger.info("Advancing to next question in quiz session {}.", sessionId);
        sendNextQuestion(sessionId);
    }

    @MessageMapping("/quiz.submitAnswer")
    public void submitAnswer(@Payload AnswerMessage answerMessage, SimpMessageHeaderAccessor headerAccessor) {
        Authentication authentication = (Authentication) headerAccessor.getUser();
        if (authentication == null || !(authentication.getPrincipal() instanceof CustomUserDetails)) {
            logger.warn("Unauthorized or invalid user submitting answer.");
            return;
        }
        CustomUserDetails userDetails = (CustomUserDetails) authentication.getPrincipal();
        Optional<User> userOptional = userRepository.findByUsername(userDetails.getUsername());
        if (userOptional.isEmpty()) {
            logger.warn("User not found for submitted answer: {}", userDetails.getUsername());
            return;
        }
        User currentUser = userOptional.get();

        Long quizSessionId = answerMessage.getQuizSessionId();
        Long questionId = answerMessage.getQuestionId();
        Long quizAttemptId = answerMessage.getQuizAttemptId();
        String selectedAnswerContent = answerMessage.getSelectedAnswerContent();
        Long responseTimeMillis = answerMessage.getResponseTimeMillis();

        Optional<QuizAttempt> attemptOptional = quizAttemptRepository.findById(quizAttemptId);
        Optional<Question> questionOptional = questionRepository.findById(questionId);

        if (attemptOptional.isEmpty() || questionOptional.isEmpty()) {
            logger.warn("Invalid attemptId {} or questionId {} for submitted answer.", quizAttemptId, questionId);
            return;
        }

        QuizAttempt quizAttempt = attemptOptional.get();
        Question question = questionOptional.get();

        boolean alreadyAnswered = quizAnswerRepository.findByQuizAttempt(quizAttempt).stream()
            .anyMatch(qa -> qa.getQuestion().getId().equals(questionId));
        
        if (alreadyAnswered) {
            logger.warn("User {} already answered question {} for attempt {}. Ignoring duplicate.", currentUser.getUsername(), questionId, quizAttemptId);
            return;
        }

        boolean isCorrect = selectedAnswerContent.equals(question.getCorrectAnswer());

        QuizAnswer quizAnswer = new QuizAnswer(quizAttempt, question, selectedAnswerContent, isCorrect, responseTimeMillis);
        quizAnswerRepository.save(quizAnswer);

        if (isCorrect) {
            quizAttempt.setCorrectAnswers(quizAttempt.getCorrectAnswers() + 1);
        }
        quizAttempt.setScore((int) Math.round(((double) quizAttempt.getCorrectAnswers() / quizAttempt.getTotalQuestions()) * 100));
        quizAttemptRepository.save(quizAttempt);

        logger.info("Answer submitted by user {}: Question ID {}, Selected '{}', Correct: {}", 
                    currentUser.getUsername(), questionId, selectedAnswerContent, isCorrect);
        
    }

    private class QuizSessionState {
        private final QuizSession quizSession;
        private final List<Question> questions;
        private int currentQuestionIndex = -1;
        private ScheduledExecutorService questionTimer;
        private boolean ended = false;

        public QuizSessionState(QuizSession quizSession, List<Question> questions) {
            this.quizSession = quizSession;
            this.questions = questions;
        }

        public QuizSession getQuizSession() {
            return quizSession;
        }

        public List<Question> getQuestions() {
            return questions;
        }

        public int getCurrentQuestionIndex() {
            return currentQuestionIndex;
        }

        public void setCurrentQuestionIndex(int currentQuestionIndex) {
            this.currentQuestionIndex = currentQuestionIndex;
        }

        public boolean isEnded() {
            return ended;
        }

        public void setEnded(boolean ended) {
            this.ended = ended;
        }

        public void stopTimer() {
            if (questionTimer != null && !questionTimer.isShutdown()) {
                questionTimer.shutdownNow();
                logger.info("Timer for quiz session {} stopped.", quizSession.getId());
            }
        }
        
        public void startNextQuestionTimer() {
            stopTimer();

            if (currentQuestionIndex < questions.size()) {
                int timeLimitSeconds = 10;
                
                final long[] timeLeft = {timeLimitSeconds};
                questionTimer = Executors.newSingleThreadScheduledExecutor();
                questionTimer.scheduleAtFixedRate(() -> {
                    if (timeLeft[0] >= 0 && !ended) {
                        QuizStatusMessage status = new QuizStatusMessage(
                            quizSession.getId(),
                            "IN_PROGRESS",
                            questions.get(currentQuestionIndex).getId(),
                            currentQuestionIndex + 1,
                            questions.size(),
                            timeLeft[0],
                            "시간이 흐르고 있습니다."
                        );
                        messagingTemplate.convertAndSend("/topic/quiz-session/" + quizSession.getId(), status);
                        timeLeft[0]--;
                    } else {
                        if (!ended) {
                            logger.info("Time's up for question {}. Advancing to next question or ending quiz for session {}.", currentQuestionIndex + 1, quizSession.getId());
                            messagingTemplate.convertAndSend("/app/quiz.nextQuestion/" + quizSession.getId(), (Object) null); 
                        }
                        stopTimer();
                    }
                }, 0, 1, TimeUnit.SECONDS);
            }
        }
    }

    private void sendNextQuestion(Long sessionId) {
        QuizSessionState state = quizSessionStates.get(sessionId);
        if (state == null) return;

        state.stopTimer();

        state.setCurrentQuestionIndex(state.getCurrentQuestionIndex() + 1);

        if (state.getCurrentQuestionIndex() < state.getQuestions().size()) {
            Question nextQuestion = state.getQuestions().get(state.getCurrentQuestionIndex());
            
            List<String> options = new ArrayList<>();
            options.add(nextQuestion.getOption1());
            options.add(nextQuestion.getOption2());
            options.add(nextQuestion.getOption3());
            options.add(nextQuestion.getOption4());
            Collections.shuffle(options, ThreadLocalRandom.current());

            QuestionMessage questionMessage = new QuestionMessage(
                sessionId,
                nextQuestion.getId(),
                nextQuestion.getContent(),
                options,
                state.getCurrentQuestionIndex() + 1,
                state.getQuestions().size(),
                10
            );
            logger.info("Sending question {} to session {}.", questionMessage.getQuestionNumber(), sessionId);
            messagingTemplate.convertAndSend("/topic/quiz-session/" + sessionId, questionMessage);

            state.startNextQuestionTimer();

        } else {
            endQuiz(sessionId);
        }
    }

    // private -> public으로 변경하여 TeacherController에서 호출 가능하도록 함
    public void endQuiz(Long sessionId) {
        QuizSessionState state = quizSessionStates.get(sessionId);
        if (state == null) return;

        state.stopTimer();
        state.setEnded(true);
        quizSessionStates.remove(sessionId);

        Optional<QuizSession> sessionOptional = quizSessionRepository.findById(sessionId);
        sessionOptional.ifPresent(s -> {
            s.setEndTime(LocalDateTime.now());
            s.setActive(false);
            quizSessionRepository.save(s);
            logger.info("Quiz session {} officially ended.", sessionId);

            QuizStatusMessage endMessage = new QuizStatusMessage(
                sessionId,
                "ENDED",
                null,
                null,
                null,
                0L,
                "퀴즈가 종료되었습니다!"
            );
            messagingTemplate.convertAndSend("/topic/quiz-session/" + sessionId, endMessage);
        });
    }

    public void shutdownScheduler() {
        scheduler.shutdown();
        logger.info("WebSocketController scheduler shut down.");
    }

    public Map<Long, QuizSessionState> getQuizSessionStates() {
        return quizSessionStates;
    }
}
