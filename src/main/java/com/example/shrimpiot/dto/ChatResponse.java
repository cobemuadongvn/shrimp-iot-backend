package com.example.shrimpiot.dto;

public class ChatResponse {
    private Long sessionId;
    private String intent;
    private ChatMessageResponse userMessage;
    private ChatMessageResponse botMessage;

    public ChatResponse(Long sessionId, String intent, ChatMessageResponse userMessage, ChatMessageResponse botMessage) {
        this.sessionId = sessionId;
        this.intent = intent;
        this.userMessage = userMessage;
        this.botMessage = botMessage;
    }

    public Long getSessionId() { return sessionId; }
    public String getIntent() { return intent; }
    public ChatMessageResponse getUserMessage() { return userMessage; }
    public ChatMessageResponse getBotMessage() { return botMessage; }
}
