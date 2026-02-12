package com.travel.planner.domain.chat.dto;

import com.travel.planner.domain.chat.entity.ChatPlan;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ChatPlanResponse {
    private String id;
    private String fileName;
    private Instant createdAt;
    /** 다운로드 URL. 7일 이내만 존재, 만료 시 null */
    private String downloadUrl;
    /** 7일 이내면 true, 지나면 false(기록만 표시) */
    private boolean downloadable;

    public static ChatPlanResponse from(ChatPlan plan, String downloadUrl, boolean downloadable) {
        return ChatPlanResponse.builder()
                .id(plan.getId())
                .fileName(plan.getFileName())
                .createdAt(plan.getCreatedAt())
                .downloadUrl(downloadUrl)
                .downloadable(downloadable)
                .build();
    }
}

