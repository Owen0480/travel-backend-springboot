package com.travel.planner.domain.chat.entity;

import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Document(collection = "chat_rooms")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ChatRoom {

    @Id
    private String id;

    @Indexed
    private String name;

    @Indexed
    private String createdByUserId;

    private String createdByUserName;

    private Instant createdAt;

    @Builder.Default
    private List<String> participantUserIds = new ArrayList<>();

    public static ChatRoom create(String name, String createdByUserId, String createdByUserName) {
        return ChatRoom.builder()
                .name(name != null && !name.isBlank() ? name : "채팅방")
                .createdByUserId(createdByUserId)
                .createdByUserName(createdByUserName)
                .createdAt(Instant.now())
                .participantUserIds(new ArrayList<>(List.of(createdByUserId)))
                .build();
    }
}
