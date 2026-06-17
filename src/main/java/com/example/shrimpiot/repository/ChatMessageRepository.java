package com.example.shrimpiot.repository;

import com.example.shrimpiot.model.ChatMessage;
import com.example.shrimpiot.model.ChatSession;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ChatMessageRepository extends JpaRepository<ChatMessage, Long> {
    List<ChatMessage> findBySessionOrderByCreatedAtAsc(ChatSession session);
}
