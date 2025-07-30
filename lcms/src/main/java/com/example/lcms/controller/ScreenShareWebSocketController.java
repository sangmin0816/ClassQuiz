package com.example.lcms.controller;

public class ScreenShareWebSocketController {
   
}
// WebSocket Controller (화면 공유 메시지 전송 예시)
@Controller
public class ScreenShareWebSocketController {

    @Autowired
    private SimpMessagingTemplate messagingTemplate;

    // 교사가 화면 캡처 이미지를 여기로 보낼 수 있습니다.
    // 예를 들어, RestController를 통해 이미지 업로드 후, 이 컨트롤러에서 WebSocket으로 전송
    public void sendScreenToStudents(byte[] imageData) {
        messagingTemplate.convertAndSend("/topic/screen", imageData);
    }
}