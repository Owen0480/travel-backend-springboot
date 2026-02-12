package com.travel.planner.domain.chat.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class PlaceRecommendResponseDto {
    private List<PlaceRecommendItemDto> places;
    private String message;
    /** LLM으로 가독성 있게 정리한 문구. 있으면 채팅에 이걸 표시 */
    @JsonProperty("formatted_message")
    private String formattedMessage;
}
