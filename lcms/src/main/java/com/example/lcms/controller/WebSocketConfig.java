package com.example.lcms.controller;

// WebSocket config (간략한 예시)


@Configuration
@EnableWebSocketMessageBroker // WebSocket 메시지 브로커를 활성화합니다.
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

    @Override
    public void configureMessageBroker(MessageBrokerRegistry config) {
        config.enableSimpleBroker("/topic"); // "/topic"으로 시작하는 메시지를 구독하는 클라이언트에게 브로드캐스트합니다.
        config.setApplicationDestinationPrefixes("/app"); // "/app"으로 시작하는 메시지는 컨트롤러로 라우팅됩니다.
    }

    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        registry.addEndpoint("/ws").withSockJS(); // WebSocket 연결 엔드포인트
    }
}