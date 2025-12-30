package com.example.demo.game2048.backend;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling


public class Game2048BackendApplication {
    public static void main(String[] args) {
        SpringApplication.run(Game2048BackendApplication.class, args);
    }
}