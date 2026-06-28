package com.example.shrimpiot.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.json.JsonMapper;

@Configuration
public class Jackson2CompatibilityConfig {

    /**
     * Spring Boot 4 uses Jackson 3 for HTTP by default, while the existing
     * MQTT and AI integrations still use Jackson 2 types. Keep one explicit
     * Jackson 2 mapper until those integrations are migrated together.
     */
    @Bean
    public ObjectMapper jackson2ObjectMapper() {
        return JsonMapper.builder()
                .findAndAddModules()
                .build();
    }
}
