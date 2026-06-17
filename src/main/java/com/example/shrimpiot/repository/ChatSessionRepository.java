package com.example.shrimpiot.repository;

import com.example.shrimpiot.model.ChatSession;
import com.example.shrimpiot.model.UserAccount;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ChatSessionRepository extends JpaRepository<ChatSession, Long> {
    List<ChatSession> findByUserOrderByUpdatedAtDesc(UserAccount user);
    List<ChatSession> findAllByOrderByUpdatedAtDesc();
}
