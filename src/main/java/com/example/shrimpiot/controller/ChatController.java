package com.example.shrimpiot.controller;

import com.example.shrimpiot.dto.*;
import com.example.shrimpiot.service.ChatService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/chat")
public class ChatController {

    private final ChatService chatService;

    public ChatController(ChatService chatService) {
        this.chatService = chatService;
    }

    @PostMapping("/message")
    public ResponseEntity<ApiResponse<ChatResponse>> sendMessage(
            @RequestHeader(value = "Authorization", required = false) String authorization,
            @Valid @RequestBody ChatRequest request
    ) {
        return ResponseEntity.ok(ApiResponse.ok("Chat response", chatService.sendMessage(authorization, request)));
    }

    @GetMapping("/sessions")
    public ResponseEntity<ApiResponse<List<ChatSessionResponse>>> getSessions(
            @RequestHeader(value = "Authorization", required = false) String authorization
    ) {
        return ResponseEntity.ok(ApiResponse.ok("Chat sessions", chatService.getSessions(authorization)));
    }

    @GetMapping("/sessions/{sessionId}/messages")
    public ResponseEntity<ApiResponse<List<ChatMessageResponse>>> getMessages(
            @RequestHeader(value = "Authorization", required = false) String authorization,
            @PathVariable Long sessionId
    ) {
        return ResponseEntity.ok(ApiResponse.ok("Chat messages", chatService.getMessages(authorization, sessionId)));
    }
}
