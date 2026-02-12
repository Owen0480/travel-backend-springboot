package com.travel.planner.domain.chat.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 일정 생성 결과. 성공 시 plan만, 추가 정보 요청 시 needMoreInfoMessage만 설정됨.
 */
@Getter
@NoArgsConstructor
@AllArgsConstructor
public class GeneratePlanResult {
    private ChatPlanResponse plan;
    private String needMoreInfoMessage;

    public static GeneratePlanResult success(ChatPlanResponse plan) {
        return new GeneratePlanResult(plan, null);
    }

    public static GeneratePlanResult needMoreInfo(String message) {
        return new GeneratePlanResult(null, message);
    }

    public boolean isNeedMoreInfo() {
        return needMoreInfoMessage != null && !needMoreInfoMessage.isBlank();
    }
}
