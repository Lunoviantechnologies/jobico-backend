package com.example.jobico;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;

@SpringBootApplication
@EnableAsync
public class JobicoApplication {
    public static void main(String[] args) {
        SpringApplication.run(JobicoApplication.class, args);
    }
}
