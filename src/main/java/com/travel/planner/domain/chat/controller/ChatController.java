package com.travel.planner.domain.chat.controller;

import com.travel.planner.domain.chat.dto.ChatMessageDto;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.SendTo;
import org.springframework.stereotype.Controller;

@Controller
@RequiredArgsConstructor
public class ChatController {

    @MessageMapping("/chat.sendMessage")
    @SendTo("/topic/public")
    public ChatMessageDto sendMessage(ChatMessageDto chatMessageDto) {
        return chatMessageDto;
    }

    @MessageMapping("/chat.addUser")
    @SendTo("/topic/public")
    public ChatMessageDto addUser(ChatMessageDto chatMessageDto) {
        chatMessageDto.setType(ChatMessageDto.MessageType.JOIN);
        return chatMessageDto;
    }
}
