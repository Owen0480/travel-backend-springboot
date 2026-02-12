package com.travel.planner.domain.chat.entity;

import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;

@Document(collection = "chat_plans")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ChatPlan {

    /** 다운로드 가능 기간(일). 이 경과 후 PDF는 삭제되고 기록만 남김 */
    public static final int DOWNLOAD_EXPIRY_DAYS = 7;

    @Id
    private String id;

    @Indexed
    private String roomId;

    private String requestedByUserId;

    private String fileName;

    /** GridFS에 저장된 PDF 파일 ID. null이면 만료되어 삭제된 상태(기록만 유지) */
    private String gridFsFileId;

    /** @deprecated 레거시: 로컬 파일 경로. 신규는 gridFsFileId 사용 */
    @Deprecated
    private String filePath;

    @Indexed
    private Instant createdAt;

    public static ChatPlan of(String roomId, String requestedByUserId, String fileName, String gridFsFileId) {
        return ChatPlan.builder()
                .roomId(roomId)
                .requestedByUserId(requestedByUserId)
                .fileName(fileName)
                .gridFsFileId(gridFsFileId)
                .createdAt(Instant.now())
                .build();
    }
}

