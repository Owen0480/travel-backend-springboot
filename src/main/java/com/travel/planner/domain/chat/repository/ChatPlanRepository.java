package com.travel.planner.domain.chat.repository;

import com.travel.planner.domain.chat.entity.ChatPlan;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;

public interface ChatPlanRepository extends MongoRepository<ChatPlan, String> {

    List<ChatPlan> findByRoomIdOrderByCreatedAtDesc(String roomId);
}

