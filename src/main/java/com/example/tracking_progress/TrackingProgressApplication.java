package com.example.tracking_progress;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling

public class TrackingProgressApplication {
    
    public static void main(String[] args) {
        SpringApplication.run(TrackingProgressApplication.class, args);
    }
}
