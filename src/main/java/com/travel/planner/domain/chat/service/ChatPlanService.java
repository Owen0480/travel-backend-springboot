package com.travel.planner.domain.chat.service;

import com.travel.planner.domain.chat.dto.ChatPlanResponse;
import com.travel.planner.domain.chat.dto.GeneratePlanResult;
import com.travel.planner.domain.chat.dto.PlanGenerateResponseDto;
import com.travel.planner.domain.chat.dto.PlaceRecommendItemDto;
import com.travel.planner.domain.chat.dto.PlaceRecommendResponseDto;
import com.travel.planner.domain.chat.entity.ChatMessage;
import com.travel.planner.domain.chat.entity.ChatPlan;
import com.travel.planner.domain.chat.repository.ChatMessageRepository;
import com.travel.planner.domain.chat.repository.ChatPlanRepository;
import com.travel.planner.global.exception.BusinessException;
import com.travel.planner.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.bson.types.ObjectId;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.gridfs.GridFsResource;
import org.springframework.data.mongodb.gridfs.GridFsTemplate;

import com.mongodb.client.gridfs.model.GridFSFile;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;

import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Base64;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class ChatPlanService {

    private static final String CONTENT_TYPE_PDF = "application/pdf";

    private final ChatPlanRepository chatPlanRepository;
    private final ChatMessageRepository chatMessageRepository;
    private final WebClient webClient;
    private final GridFsTemplate gridFsTemplate;

    @Value("${external.fastapi.url2}")
    private String fastApiBaseUrl;

    @Value("${planner.plan.base-dir:files/plans}")
    private String planBaseDir;

    /**
     * FastAPI(LangGraph)를 WebClient(논블로킹)로 호출해 일정 PDF를 생성합니다.
     * 채팅방별로 스레드를 블로킹하지 않아 동시 요청에 더 효율적입니다.
     */
    public Mono<GeneratePlanResult> generatePlanAsync(String roomId, String requesterUserId) {
        return Mono.fromCallable(() -> buildPlanRequestBody(roomId))
                .subscribeOn(Schedulers.boundedElastic())
                .flatMap(body -> {
                    log.info("Calling FastAPI planner: {}/api/v1/plan/generate", fastApiBaseUrl);
                    return webClient.post()
                            .uri(fastApiBaseUrl + "/api/v1/plan/generate")
                            .bodyValue(body)
                            .retrieve()
                            .onStatus(status -> status.value() >= 400, resp -> Mono.just(
                                    new BusinessException(ErrorCode.INTERNAL_SERVER_ERROR, "일정 생성 API 호출에 실패했습니다.")))
                            .bodyToMono(PlanGenerateResponseDto.class);
                })
                .flatMap(dto -> Mono.fromCallable(() -> processPlanResponse(dto, roomId, requesterUserId))
                        .subscribeOn(Schedulers.boundedElastic()))
                .onErrorResume(WebClientResponseException.class, e -> {
                    log.warn("FastAPI plan generate request failed: {}", e.getMessage());
                    return Mono.just(GeneratePlanResult.needMoreInfo(
                            "일정 생성 API에 연결할 수 없거나 응답이 없습니다. FastAPI 서버가 실행 중인지 확인해 주세요."));
                })
                .onErrorResume(BusinessException.class, e -> Mono.error(e));
    }

    /**
     * FastAPI 장소 추천 API를 호출해 상위 3곳을 조회하고, 채팅에 보낼 메시지 문자열로 포맷합니다.
     * 예: "부산 분위기 좋은곳 추천해줘" -> query="부산 분위기 좋은곳"
     */
    public Mono<String> recommendPlacesAsync(String query) {
        if (query == null || query.isBlank()) {
            return Mono.just("검색어를 입력해 주세요. 예: 부산 분위기 좋은곳 추천해줘");
        }
        Map<String, Object> body = new HashMap<>();
        body.put("query", query.trim());
        body.put("language", "ko");
        return webClient.post()
                .uri(fastApiBaseUrl + "/api/v1/places/recommend")
                .bodyValue(body)
                .retrieve()
                .onStatus(status -> status.value() >= 400, resp -> Mono.just(new RuntimeException("장소 추천 API 호출 실패")))
                .bodyToMono(PlaceRecommendResponseDto.class)
                .map(this::recommendMessageToSend)
                .onErrorResume(WebClientResponseException.class, e -> {
                    log.warn("FastAPI place recommend request failed: {}", e.getMessage());
                    return Mono.just("장소 추천 API에 연결할 수 없습니다. 잠시 후 다시 시도해 주세요.");
                })
                .onErrorReturn("장소 추천 중 오류가 발생했습니다. 잠시 후 다시 시도해 주세요.");
    }

    /**
     * 채팅에 보낼 추천 메시지 결정: FastAPI가 LLM으로 만든 formattedMessage가 있으면 사용,
     * 없으면 기존 포맷으로 생성.
     */
    private String recommendMessageToSend(PlaceRecommendResponseDto dto) {
        if (dto.getFormattedMessage() != null && !dto.getFormattedMessage().isBlank()) {
            return dto.getFormattedMessage().trim();
        }
        return formatRecommendMessage(dto);
    }

    private String formatRecommendMessage(PlaceRecommendResponseDto dto) {
        if (dto.getPlaces() == null || dto.getPlaces().isEmpty()) {
            return dto.getMessage() != null ? dto.getMessage() : "검색 결과가 없습니다.";
        }
        StringBuilder sb = new StringBuilder();
        sb.append("📍 추천 장소 상위 3곳\n\n");
        int i = 1;
        for (PlaceRecommendItemDto p : dto.getPlaces()) {
            sb.append(i++).append(". **").append(p.getName() != null ? p.getName() : "").append("**\n");
            if (p.getAddress() != null && !p.getAddress().isEmpty()) {
                sb.append("   주소: ").append(p.getAddress()).append("\n");
            }
            if (p.getRating() != null) {
                sb.append("   평점: ").append(p.getRating());
                if (p.getUserRatingsTotal() != null) {
                    sb.append(" (리뷰 ").append(p.getUserRatingsTotal()).append("개)");
                }
                sb.append("\n");
            }
            sb.append("\n");
        }
        return sb.toString().trim();
    }

    private Map<String, Object> buildPlanRequestBody(String roomId) {
        List<ChatMessage> messages = chatMessageRepository
                .findByRoomIdOrderByCreatedAtAsc(roomId, PageRequest.of(0, 100));
        List<Map<String, String>> history = messages.stream()
                .map(m -> {
                    Map<String, String> map = new HashMap<>();
                    map.put("sender", m.getSenderUserName());
                    map.put("content", m.getContent());
                    return map;
                })
                .collect(Collectors.toList());
        Map<String, Object> body = new HashMap<>();
        body.put("room_id", roomId);
        body.put("messages", history);
        body.put("language", "ko");
        return body;
    }

    private GeneratePlanResult processPlanResponse(PlanGenerateResponseDto dto, String roomId, String requesterUserId) {
        if (Boolean.TRUE.equals(dto.getNeedMoreInfo()) && dto.getMessage() != null) {
            return GeneratePlanResult.needMoreInfo(dto.getMessage());
        }
        String pdfBase64 = dto.getPdfBase64();
        String fileName = dto.getFileName();
        if (pdfBase64 == null || fileName == null) {
            log.warn("FastAPI returned need_more_info=false but fileName or pdfBase64 is null. roomId={}", roomId);
            return GeneratePlanResult.needMoreInfo("일정 생성 API 응답이 올바르지 않습니다. 잠시 후 다시 시도해 주세요.");
        }
        try {
            byte[] pdfBytes = Base64.getDecoder().decode(pdfBase64);
            String gridFsFileId = storePdfInGridFs(fileName, pdfBytes);
            ChatPlan plan = ChatPlan.of(roomId, requesterUserId, fileName, gridFsFileId);
            plan = chatPlanRepository.save(plan);
            String downloadUrl = String.format("/api/v1/chat/rooms/%s/plans/%s/download", roomId, plan.getId());
            return GeneratePlanResult.success(ChatPlanResponse.from(plan, downloadUrl, true));
        } catch (Exception e) {
            log.error("Failed to save plan PDF for room {}", roomId, e);
            return GeneratePlanResult.needMoreInfo("일정 저장 중 오류가 발생했습니다. 잠시 후 다시 시도해 주세요.");
        }
    }

    private String storePdfInGridFs(String fileName, byte[] pdfBytes) {
        ObjectId objectId = gridFsTemplate.store(
                new ByteArrayInputStream(pdfBytes),
                fileName,
                CONTENT_TYPE_PDF
        );
        return objectId.toHexString();
    }

    /** 7일 경과 시 GridFS에서 PDF 삭제하고 gridFsFileId 제거(기록만 유지) */
    private void expirePlanIfNeeded(ChatPlan plan) {
        if (plan.getGridFsFileId() == null) {
            return;
        }
        Instant expiry = plan.getCreatedAt().plus(ChatPlan.DOWNLOAD_EXPIRY_DAYS, ChronoUnit.DAYS);
        if (Instant.now().isBefore(expiry)) {
            return;
        }
        try {
            gridFsTemplate.delete(new Query(Criteria.where("_id").is(new ObjectId(plan.getGridFsFileId()))));
            plan.setGridFsFileId(null);
            chatPlanRepository.save(plan);
            log.info("Plan PDF expired and removed from DB: planId={}", plan.getId());
        } catch (Exception e) {
            log.warn("Failed to expire plan PDF: planId={}", plan.getId(), e);
        }
    }

    private boolean isDownloadable(ChatPlan plan) {
        if (plan.getGridFsFileId() != null) {
            Instant expiry = plan.getCreatedAt().plus(ChatPlan.DOWNLOAD_EXPIRY_DAYS, ChronoUnit.DAYS);
            return Instant.now().isBefore(expiry);
        }
        if (plan.getFilePath() != null) {
            Instant expiry = plan.getCreatedAt().plus(ChatPlan.DOWNLOAD_EXPIRY_DAYS, ChronoUnit.DAYS);
            return Instant.now().isBefore(expiry);
        }
        return false;
    }

    public List<ChatPlanResponse> listPlans(String roomId) {
        return chatPlanRepository.findByRoomIdOrderByCreatedAtDesc(roomId).stream()
                .map(plan -> {
                    expirePlanIfNeeded(plan);
                    boolean downloadable = isDownloadable(plan);
                    String downloadUrl = downloadable
                            ? String.format("/api/v1/chat/rooms/%s/plans/%s/download", roomId, plan.getId())
                            : null;
                    return ChatPlanResponse.from(plan, downloadUrl, downloadable);
                })
                .collect(Collectors.toList());
    }

    /**
     * 다운로드 가능 기간(7일) 내일 때만 PDF 바이트 반환. 만료 시 예외.
     */
    public byte[] getPlanBytes(String roomId, String planId) throws IOException {
        ChatPlan plan = chatPlanRepository.findById(planId)
                .orElseThrow(() -> new BusinessException(ErrorCode.ENTITY_NOT_FOUND, "일정 PDF를 찾을 수 없습니다."));
        if (!plan.getRoomId().equals(roomId)) {
            throw new BusinessException(ErrorCode.ACCESS_DENIED, "다른 방의 일정에는 접근할 수 없습니다.");
        }
        expirePlanIfNeeded(plan);

        if (plan.getGridFsFileId() != null && isDownloadable(plan)) {
            GridFSFile file = gridFsTemplate.findOne(
                    new Query(Criteria.where("_id").is(new ObjectId(plan.getGridFsFileId()))));
            if (file != null) {
                GridFsResource resource = gridFsTemplate.getResource(file);
                if (resource.exists()) {
                    try (InputStream is = resource.getInputStream()) {
                        return is.readAllBytes();
                    }
                }
            }
        }
        if (plan.getFilePath() != null && isDownloadable(plan)) {
            Path path = Path.of(planBaseDir, plan.getRoomId(), plan.getFileName());
            if (Files.exists(path)) {
                return Files.readAllBytes(path);
            }
        }
        throw new BusinessException(ErrorCode.ENTITY_NOT_FOUND, "다운로드 가능 기간(7일)이 지났거나 파일이 없습니다.");
    }

    public ChatPlan getPlan(String roomId, String planId) {
        ChatPlan plan = chatPlanRepository.findById(planId)
                .orElseThrow(() -> new BusinessException(ErrorCode.ENTITY_NOT_FOUND, "일정 PDF를 찾을 수 없습니다."));
        if (!plan.getRoomId().equals(roomId)) {
            throw new BusinessException(ErrorCode.ACCESS_DENIED, "다른 방의 일정에는 접근할 수 없습니다.");
        }
        return plan;
    }

    /** 해당 채팅방의 모든 일정(PDF)을 GridFS 및 DB에서 삭제. 방 삭제 시 호출. */
    public void deleteAllPlansByRoomId(String roomId) {
        List<ChatPlan> plans = chatPlanRepository.findByRoomIdOrderByCreatedAtDesc(roomId);
        for (ChatPlan plan : plans) {
            if (plan.getGridFsFileId() != null) {
                try {
                    gridFsTemplate.delete(new Query(Criteria.where("_id").is(new ObjectId(plan.getGridFsFileId()))));
                } catch (Exception e) {
                    log.warn("Failed to delete GridFS file for plan: planId={}", plan.getId(), e);
                }
            }
        }
        chatPlanRepository.deleteByRoomId(roomId);
    }
}
