package com.travel.planner.domain.chat.dto;

import com.travel.planner.domain.chat.entity.ChatMessage;
import lombok.*;

import java.time.Instant;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ChatMessageResponse {
    private String id;
    private String roomId;
    private String senderUserId;
    private String senderUserName;
    private String content;
    private Instant createdAt;

    public static ChatMessageResponse from(ChatMessage msg) {
        return ChatMessageResponse.builder()
                .id(msg.getId())
                .roomId(msg.getRoomId())
                .senderUserId(msg.getSenderUserId())
                .senderUserName(msg.getSenderUserName())
                .content(msg.getContent())
                .createdAt(msg.getCreatedAt())
                .build();
    }
}
