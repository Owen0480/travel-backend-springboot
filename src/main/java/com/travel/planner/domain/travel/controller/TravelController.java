package com.travel.planner.domain.travel.controller;

import com.travel.planner.global.common.dto.BaseResponse;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestTemplate;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/travel")
public class TravelController {

    private final RestTemplate restTemplate = new RestTemplate();

    /**
     * travel-frontend → travel-backend-springboot → backend-fastapi 전체 플로우 테스트용 엔드포인트
     */
    @PostMapping("/test")
    public ResponseEntity<BaseResponse> test(@RequestBody Map<String, Object> body) {
        String message = body.getOrDefault("message", "테스트 메시지").toString();

        String fastApiUrl = "http://localhost:8000/api/v1/travel/travel";

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        Map<String, Object> requestBody = Map.of("message", message);
        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(requestBody, headers);

        ResponseEntity<Map> fastApiResponse = restTemplate.exchange(
                fastApiUrl,
                HttpMethod.POST,
                entity,
                new ParameterizedTypeReference<Map>() {}
        );
        Map response = fastApiResponse.getBody();

        return ResponseEntity.ok(BaseResponse.success(response));
    }
}

