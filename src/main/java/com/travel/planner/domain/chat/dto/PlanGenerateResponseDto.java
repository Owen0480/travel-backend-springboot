package com.travel.planner.domain.chat.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class PlanGenerateResponseDto {
    /** true면 추가 정보 요청(되물어보기), message 사용. false면 fileName + pdfBase64 사용 */
    @JsonProperty("need_more_info")
    private Boolean needMoreInfo;
    private String message;
    private String fileName;
    private String pdfBase64;
}

