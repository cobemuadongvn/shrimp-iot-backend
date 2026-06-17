package com.example.shrimpiot.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class ApiKeyService {

    @Value("${iot.api-key}")
    private String validApiKey;

    public void validate(String apiKey) {
        if (apiKey == null || apiKey.isBlank() || !apiKey.equals(validApiKey)) {
            throw new IllegalArgumentException("Invalid API key");
        }
    }
}
