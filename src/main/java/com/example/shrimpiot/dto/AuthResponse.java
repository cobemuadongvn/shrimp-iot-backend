package com.example.shrimpiot.dto;

import java.time.LocalDateTime;

public class AuthResponse {

    private String token;
    private String tokenType = "Bearer";
    private LocalDateTime expiresAt;
    private UserResponse user;

    public AuthResponse() {
    }

    public AuthResponse(String token, LocalDateTime expiresAt, UserResponse user) {
        this.token = token;
        this.expiresAt = expiresAt;
        this.user = user;
    }

    public String getToken() { return token; }
    public String getTokenType() { return tokenType; }
    public LocalDateTime getExpiresAt() { return expiresAt; }
    public UserResponse getUser() { return user; }
}
