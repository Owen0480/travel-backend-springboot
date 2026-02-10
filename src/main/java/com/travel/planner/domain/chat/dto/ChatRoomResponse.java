package com.travel.planner.domain.chat.dto;

import com.travel.planner.domain.chat.entity.ChatRoom;
import lombok.*;

import java.time.Instant;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ChatRoomResponse {
    private String id;
    private String name;
    private String createdByUserId;
    private String createdByUserName;
    private Instant createdAt;
    private List<String> participantUserIds;

    public static ChatRoomResponse from(ChatRoom room) {
        return ChatRoomResponse.builder()
                .id(room.getId())
                .name(room.getName())
                .createdByUserId(room.getCreatedByUserId())
                .createdByUserName(room.getCreatedByUserName())
                .createdAt(room.getCreatedAt())
                .participantUserIds(room.getParticipantUserIds() != null ? room.getParticipantUserIds() : List.of())
                .build();
    }
}
