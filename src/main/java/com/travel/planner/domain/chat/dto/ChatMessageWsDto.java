package com.travel.planner.domain.chat.dto;

import lombok.*;

import java.time.Instant;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ChatMessageWsDto {
    private String id;
    private String roomId;
    private String senderUserId;
    private String senderUserName;
    private String content;
    private Instant createdAt;
}
