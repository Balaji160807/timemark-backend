package com.timemark;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class TimemarkApplication {
    public static void main(String[] args) {
        SpringApplication.run(TimemarkApplication.class, args);
    }
}
