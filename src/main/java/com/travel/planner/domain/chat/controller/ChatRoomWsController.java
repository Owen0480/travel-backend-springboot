package com.travel.planner.domain.chat.controller;

import com.travel.planner.domain.chat.dto.ChatMessageWsDto;
import com.travel.planner.domain.chat.dto.ChatSendRequest;
import com.travel.planner.domain.chat.dto.GeneratePlanResult;
import com.travel.planner.domain.chat.service.ChatRoomService;
import com.travel.planner.domain.chat.service.ChatPlanService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;
import reactor.core.scheduler.Schedulers;

@Slf4j
@Controller
@RequiredArgsConstructor
public class ChatRoomWsController {

    private final ChatRoomService chatRoomService;
    private final ChatPlanService chatPlanService;
    private final SimpMessagingTemplate messagingTemplate;

    private static final String TOPIC_PREFIX = "/topic/chat/room/";

    @MessageMapping("/chat/room/{roomId}")
    public void sendMessage(@DestinationVariable String roomId, @Payload ChatSendRequest request) {
        if (request == null || request.getContent() == null || request.getContent().isBlank()) {
            return;
        }
        String senderUserId = request.getSenderUserId() != null ? request.getSenderUserId() : "anonymous";
        String senderUserName = request.getSenderUserName() != null ? request.getSenderUserName() : "익명";
        ChatMessageWsDto dto = chatRoomService.saveAndToWsDto(roomId, senderUserId, senderUserName, request.getContent().trim());
        messagingTemplate.convertAndSend(TOPIC_PREFIX + roomId, dto);

        String contentTrimmed = request.getContent().trim();
        String normalized = contentTrimmed.replaceAll("\\s+", "");

        // 장소 추천 트리거 (예: "부산 분위기 좋은곳 추천해줘", "부산 인기 많은 곳 추천해줘") — 일정 추천과 구분
        boolean isPlaceRecommend = (normalized.contains("추천해줘") || normalized.contains("추천해주세요") || normalized.contains("추천해"))
                && !normalized.contains("일정추천") && !normalized.contains("일정짜줘") && !normalized.contains("일정만들어");
        if (isPlaceRecommend) {
            String query = contentTrimmed
                    .replaceAll("\\s*추천\\s*해\\s*줘\\s*$", "")
                    .replaceAll("\\s*추천\\s*해\\s*주세요\\s*$", "")
                    .replaceAll("\\s*추천\\s*해\\s*$", "")
                    .replaceAll("\\s*추천\\s*해줘\\s*$", "")
                    .replaceAll("\\s*추천\\s*$", "")
                    .trim();
            if (query.isEmpty()) query = contentTrimmed;
            final String recommendQuery = query;
            chatPlanService.recommendPlacesAsync(recommendQuery)
                    .subscribeOn(Schedulers.boundedElastic())
                    .subscribe(
                            message -> {
                                ChatMessageWsDto plannerMsg = chatRoomService.saveAndToWsDto(
                                        roomId, null, "PLANNER", message);
                                messagingTemplate.convertAndSend(TOPIC_PREFIX + roomId, plannerMsg);
                            },
                            ex -> {
                                log.error("Place recommend failed for room {}", roomId, ex);
                                ChatMessageWsDto errorMsg = chatRoomService.saveAndToWsDto(
                                        roomId, null, "PLANNER", "장소 추천 중 오류가 발생했습니다. 잠시 후 다시 시도해 주세요.");
                                messagingTemplate.convertAndSend(TOPIC_PREFIX + roomId, errorMsg);
                            }
                    );
        }

        // 일정 생성 트리거 (공백 제거 후 키워드 포함 여부로 판단)
        boolean triggerPlan = normalized.contains("일정생성해줘") || normalized.contains("일정생성")
                || normalized.contains("일정보여줘") || normalized.contains("일정을짜줘") || normalized.contains("일정짜줘")
                || normalized.contains("일정만들어줘") || normalized.contains("일정만들어")
                || normalized.contains("일정뽑아줘") || normalized.contains("일정추천해줘") || normalized.contains("일정추천");
        if (triggerPlan) {
            chatPlanService.generatePlanAsync(roomId, senderUserId)
                    .subscribeOn(Schedulers.boundedElastic())
                    .subscribe(
                            result -> {
                                if (result.isNeedMoreInfo()) {
                                    ChatMessageWsDto plannerMsg = chatRoomService.saveAndToWsDto(
                                            roomId, null, "PLANNER", result.getNeedMoreInfoMessage());
                                    messagingTemplate.convertAndSend(TOPIC_PREFIX + roomId, plannerMsg);
                                } else {
                                    ChatMessageWsDto systemMsg = ChatMessageWsDto.builder()
                                            .id(null)
                                            .roomId(roomId)
                                            .senderUserId(null)
                                            .senderUserName("PLANNER")
                                            .content("PLAN_READY")
                                            .createdAt(java.time.Instant.now())
                                            .build();
                                    messagingTemplate.convertAndSend(TOPIC_PREFIX + roomId, systemMsg);
                                }
                            },
                            ex -> {
                                log.error("Failed to generate plan for room {}", roomId, ex);
                                ChatMessageWsDto errorMsg = ChatMessageWsDto.builder()
                                        .id(null)
                                        .roomId(roomId)
                                        .senderUserId(null)
                                        .senderUserName("PLANNER")
                                        .content("일정 생성 중 오류가 발생했습니다. 잠시 후 다시 시도해 주세요.")
                                        .createdAt(java.time.Instant.now())
                                        .build();
                                messagingTemplate.convertAndSend(TOPIC_PREFIX + roomId, errorMsg);
                            }
                    );
        }
    }
}
