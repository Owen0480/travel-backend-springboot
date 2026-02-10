package com.travel.planner.domain.chat.controller;

import com.travel.planner.domain.chat.dto.ChatMessageWsDto;
import com.travel.planner.domain.chat.dto.ChatSendRequest;
import com.travel.planner.domain.chat.service.ChatRoomService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;

@Slf4j
@Controller
@RequiredArgsConstructor
public class ChatRoomWsController {

    private final ChatRoomService chatRoomService;
    private final SimpMessagingTemplate messagingTemplate;

    private static final String TOPIC_PREFIX = "/topic/chat/room/";

    @MessageMapping("/chat/room/{roomId}")
    public void sendMessage(@DestinationVariable String roomId, @Payload ChatSendRequest request) {
        if (request == null || request.getContent() == null || request.getContent().isBlank()) {
            return;
        }
        String senderUserId = request.getSenderUserId() != null ? request.getSenderUserId() : "anonymous";
        String senderUserName = request.getSenderUserName() != null ? request.getSenderUserName() : "익명";
        ChatMessageWsDto dto = chatRoomService.saveAndToWsDto(roomId, senderUserId, senderUserName, request.getContent().trim());
        messagingTemplate.convertAndSend(TOPIC_PREFIX + roomId, dto);
    }
}
