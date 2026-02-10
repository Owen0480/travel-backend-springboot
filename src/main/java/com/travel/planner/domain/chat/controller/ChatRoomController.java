package com.travel.planner.domain.chat.controller;

import com.travel.planner.domain.chat.dto.ChatMessageResponse;
import com.travel.planner.domain.chat.dto.ChatRoomCreateRequest;
import com.travel.planner.domain.chat.dto.ChatRoomResponse;
import com.travel.planner.domain.chat.service.ChatRoomService;
import com.travel.planner.domain.user.dto.UserInfoResponse;
import com.travel.planner.domain.user.service.UserService;
import com.travel.planner.global.common.dto.BaseResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "ChatRoom", description = "채팅방 API (방별 URL, 친구 초대)")
@RestController
@RequestMapping("/api/v1/chat/rooms")
@RequiredArgsConstructor
public class ChatRoomController {

    private final ChatRoomService chatRoomService;
    private final UserService userService;

    @Operation(summary = "채팅방 생성", description = "새 채팅방을 만들고 URL로 친구를 초대할 수 있습니다.")
    @PostMapping
    public ResponseEntity<BaseResponse<ChatRoomResponse>> create(@RequestBody(required = false) ChatRoomCreateRequest request) {
        UserInfoResponse user = userService.getCurrentUserInfo();
        ChatRoomResponse room = chatRoomService.createRoom(
                String.valueOf(user.getUserId()),
                user.getFullName(),
                request != null ? request : new ChatRoomCreateRequest()
        );
        return ResponseEntity.ok(BaseResponse.success(room));
    }

    @Operation(summary = "내 채팅방 목록", description = "참여 중인 채팅방 목록")
    @GetMapping
    public ResponseEntity<BaseResponse<List<ChatRoomResponse>>> list() {
        UserInfoResponse user = userService.getCurrentUserInfo();
        List<ChatRoomResponse> rooms = chatRoomService.listMyRooms(String.valueOf(user.getUserId()));
        return ResponseEntity.ok(BaseResponse.success(rooms));
    }

    @Operation(summary = "채팅방 조회 (참여)", description = "방 URL로 들어오면 자동 참여되고 방 정보 반환")
    @GetMapping("/{roomId}")
    public ResponseEntity<BaseResponse<ChatRoomResponse>> get(@PathVariable String roomId) {
        UserInfoResponse user = userService.getCurrentUserInfo();
        ChatRoomResponse room = chatRoomService.getRoom(roomId, String.valueOf(user.getUserId()));
        return ResponseEntity.ok(BaseResponse.success(room));
    }

    @Operation(summary = "채팅방 이름 변경", description = "방장만 채팅방 이름을 변경할 수 있습니다.")
    @PutMapping("/{roomId}")
    public ResponseEntity<BaseResponse<ChatRoomResponse>> rename(
            @PathVariable String roomId,
            @RequestBody ChatRoomCreateRequest request
    ) {
        UserInfoResponse user = userService.getCurrentUserInfo();
        ChatRoomResponse room = chatRoomService.renameRoom(
                roomId,
                String.valueOf(user.getUserId()),
                request != null ? request.getName() : null
        );
        return ResponseEntity.ok(BaseResponse.success(room));
    }

    @Operation(summary = "채팅 메시지 목록", description = "채팅방 메시지 조회 (과거 대화)")
    @GetMapping("/{roomId}/messages")
    public ResponseEntity<BaseResponse<List<ChatMessageResponse>>> messages(
            @PathVariable String roomId,
            @RequestParam(defaultValue = "100") int limit
    ) {
        List<ChatMessageResponse> messages = chatRoomService.getMessages(roomId, Math.min(limit, 500));
        return ResponseEntity.ok(BaseResponse.success(messages));
    }
}
