package com.example.shrimpiot;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class ShrimpIotApplication {

    public static void main(String[] args) {
        SpringApplication.run(ShrimpIotApplication.class, args);
    }
}
