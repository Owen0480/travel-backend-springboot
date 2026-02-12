package com.travel.planner.domain.chat.controller;

import com.travel.planner.domain.chat.dto.ChatPlanResponse;
import com.travel.planner.domain.chat.service.ChatPlanService;
import com.travel.planner.global.common.dto.BaseResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import com.travel.planner.domain.chat.entity.ChatPlan;

import java.io.IOException;
import java.util.List;

@Tag(name = "ChatPlan", description = "채팅방 일정 PDF API")
@RestController
@RequestMapping("/api/v1/chat/rooms/{roomId}/plans")
@RequiredArgsConstructor
public class ChatPlanController {

    private final ChatPlanService chatPlanService;

    @Operation(summary = "방의 일정 PDF 목록", description = "채팅방에서 생성된 일정 PDF 목록을 조회합니다.")
    @GetMapping
    public ResponseEntity<BaseResponse<List<ChatPlanResponse>>> list(@PathVariable String roomId) {
        List<ChatPlanResponse> plans = chatPlanService.listPlans(roomId);
        return ResponseEntity.ok(BaseResponse.success(plans));
    }

    @Operation(summary = "일정 PDF 다운로드", description = "생성 후 7일 이내만 다운로드 가능합니다.")
    @GetMapping("/{planId}/download")
    public ResponseEntity<ByteArrayResource> download(@PathVariable String roomId, @PathVariable String planId) throws IOException {
        byte[] bytes = chatPlanService.getPlanBytes(roomId, planId);
        ChatPlan plan = chatPlanService.getPlan(roomId, planId);

        ByteArrayResource resource = new ByteArrayResource(bytes);
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_PDF);
        headers.setContentDispositionFormData("attachment", plan.getFileName() != null ? plan.getFileName() : "plan.pdf");

        return ResponseEntity.ok()
                .headers(headers)
                .body(resource);
    }
}

