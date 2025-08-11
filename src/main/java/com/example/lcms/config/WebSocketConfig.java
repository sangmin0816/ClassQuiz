package com.example.lcms.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

@Configuration
@EnableWebSocketMessageBroker // 웹소켓 메시지 브로커 기능 활성화
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

    @Override
    public void configureMessageBroker(MessageBrokerRegistry config) {
        // "/topic"으로 시작하는 메시지를 브로커가 처리하도록 설정합니다.
        // 클라이언트는 "/topic/quiz-session/{sessionId}"와 같은 경로로 메시지를 구독할 수 있습니다.
        config.enableSimpleBroker("/topic");
        
        // "/app"으로 시작하는 메시지는 메시지 매핑된 @Controller로 라우팅됩니다.
        // 클라이언트가 서버로 메시지를 보낼 때 사용될 프리픽스입니다.
        config.setApplicationDestinationPrefixes("/app");
    }

    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        // 웹소켓 연결을 위한 STOMP 엔드포인트를 등록합니다.
        // 클라이언트가 "/ws" 경로로 웹소켓 연결을 시도할 수 있도록 합니다.
        // .withSockJS()는 웹소켓을 지원하지 않는 브라우저를 위해 SockJS 폴백 옵션을 추가합니다.
        registry.addEndpoint("/ws").withSockJS();
    }
}
