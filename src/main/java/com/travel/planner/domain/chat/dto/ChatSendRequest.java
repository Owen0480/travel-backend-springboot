package com.travel.planner.domain.chat.dto;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ChatSendRequest {
    private String senderUserId;
    private String senderUserName;
    private String content;
}
