package com.travel.planner.domain.chat.entity;

import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;

@Document(collection = "chat_messages")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ChatMessage {

    @Id
    private String id;

    @Indexed
    private String roomId;

    private String senderUserId;
    private String senderUserName;
    private String content;

    @Indexed
    private Instant createdAt;

    public static ChatMessage of(String roomId, String senderUserId, String senderUserName, String content) {
        return ChatMessage.builder()
                .roomId(roomId)
                .senderUserId(senderUserId)
                .senderUserName(senderUserName)
                .content(content)
                .createdAt(Instant.now())
                .build();
    }
}
