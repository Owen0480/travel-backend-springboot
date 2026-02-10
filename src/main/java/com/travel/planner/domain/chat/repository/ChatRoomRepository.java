package com.travel.planner.domain.chat.repository;

import com.travel.planner.domain.chat.entity.ChatRoom;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;

public interface ChatRoomRepository extends MongoRepository<ChatRoom, String> {

    List<ChatRoom> findByParticipantUserIdsContainingOrderByCreatedAtDesc(String userId);
}
