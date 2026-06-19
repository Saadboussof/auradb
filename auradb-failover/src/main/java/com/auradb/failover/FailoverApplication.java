package com.auradb.failover;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class FailoverApplication {
    public static void main(String[] args) {
        SpringApplication.run(FailoverApplication.class, args);
    }
}
