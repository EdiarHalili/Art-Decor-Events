package com.artdecor.workforce;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class WorkforceApplication {
    public static void main(String[] args) {
        SpringApplication.run(WorkforceApplication.class, args);
    }
}
