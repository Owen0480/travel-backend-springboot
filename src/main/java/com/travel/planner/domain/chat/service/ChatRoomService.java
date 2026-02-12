package com.travel.planner.domain.chat.service;

import com.travel.planner.domain.chat.dto.ChatMessageResponse;
import com.travel.planner.domain.chat.dto.ChatMessageWsDto;
import com.travel.planner.domain.chat.dto.ChatRoomCreateRequest;
import com.travel.planner.domain.chat.dto.ChatRoomResponse;
import com.travel.planner.domain.chat.entity.ChatMessage;
import com.travel.planner.domain.chat.entity.ChatRoom;
import com.travel.planner.domain.chat.repository.ChatMessageRepository;
import com.travel.planner.domain.chat.repository.ChatRoomRepository;
import com.travel.planner.global.exception.BusinessException;
import com.travel.planner.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ChatRoomService {

    private final ChatRoomRepository chatRoomRepository;
    private final ChatMessageRepository chatMessageRepository;
    private final ChatPlanService chatPlanService;

    public ChatRoomResponse createRoom(String userId, String userName, ChatRoomCreateRequest request) {
        ChatRoom room = ChatRoom.create(
                request != null ? request.getName() : null,
                userId,
                userName != null ? userName : "User"
        );
        room = chatRoomRepository.save(room);
        return ChatRoomResponse.from(room);
    }

    public ChatRoomResponse getRoom(String roomId, String userId) {
        ChatRoom room = chatRoomRepository.findById(roomId)
                .orElseThrow(() -> new BusinessException(ErrorCode.ENTITY_NOT_FOUND, "채팅방을 찾을 수 없습니다."));
        if (room.getParticipantUserIds() != null && !room.getParticipantUserIds().contains(userId)) {
            room.getParticipantUserIds().add(userId);
            chatRoomRepository.save(room);
        }
        return ChatRoomResponse.from(room);
    }

    public List<ChatRoomResponse> listMyRooms(String userId) {
        return chatRoomRepository.findByParticipantUserIdsContainingOrderByCreatedAtDesc(userId)
                .stream()
                .map(ChatRoomResponse::from)
                .collect(Collectors.toList());
    }

    public ChatRoomResponse renameRoom(String roomId, String requesterUserId, String newName) {
        ChatRoom room = chatRoomRepository.findById(roomId)
                .orElseThrow(() -> new BusinessException(ErrorCode.ENTITY_NOT_FOUND, "채팅방을 찾을 수 없습니다."));

        // 방장만 이름 변경 가능
        if (room.getCreatedByUserId() != null && !room.getCreatedByUserId().equals(requesterUserId)) {
            throw new BusinessException(ErrorCode.ACCESS_DENIED, "채팅방 이름을 변경할 권한이 없습니다.");
        }

        String finalName = (newName != null && !newName.isBlank()) ? newName.trim() : "채팅방";
        room.setName(finalName);
        room = chatRoomRepository.save(room);
        return ChatRoomResponse.from(room);
    }

    public List<ChatMessageResponse> getMessages(String roomId, int limit) {
        return chatMessageRepository.findByRoomIdOrderByCreatedAtAsc(roomId, PageRequest.of(0, limit))
                .stream()
                .map(ChatMessageResponse::from)
                .collect(Collectors.toList());
    }

    /** 채팅방 나가기: 참여자에서 제거. 마지막 참여자가 나가면 해당 방의 메시지·PDF·채팅방을 모두 삭제합니다. */
    public void leaveRoom(String roomId, String userId) {
        ChatRoom room = chatRoomRepository.findById(roomId)
                .orElseThrow(() -> new BusinessException(ErrorCode.ENTITY_NOT_FOUND, "채팅방을 찾을 수 없습니다."));
        if (room.getParticipantUserIds() == null) {
            return;
        }
        room.getParticipantUserIds().remove(userId);
        if (room.getParticipantUserIds().isEmpty()) {
            chatMessageRepository.deleteByRoomId(roomId);
            chatPlanService.deleteAllPlansByRoomId(roomId);
            chatRoomRepository.delete(room);
        } else {
            chatRoomRepository.save(room);
        }
    }

    public ChatMessageWsDto saveAndToWsDto(String roomId, String senderUserId, String senderUserName, String content) {
        ChatRoom room = chatRoomRepository.findById(roomId)
                .orElseThrow(() -> new BusinessException(ErrorCode.ENTITY_NOT_FOUND, "채팅방을 찾을 수 없습니다."));
        ChatMessage msg = ChatMessage.of(roomId, senderUserId, senderUserName, content);
        msg = chatMessageRepository.save(msg);
        return ChatMessageWsDto.builder()
                .id(msg.getId())
                .roomId(msg.getRoomId())
                .senderUserId(msg.getSenderUserId())
                .senderUserName(msg.getSenderUserName())
                .content(msg.getContent())
                .createdAt(msg.getCreatedAt())
                .build();
    }
}
