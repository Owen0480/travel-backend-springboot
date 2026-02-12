package com.travel.planner.domain.chat.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class PlaceRecommendItemDto {
    private String name;
    private String address;
    private Double rating;
    @JsonProperty("user_ratings_total")
    private Integer userRatingsTotal;
}
