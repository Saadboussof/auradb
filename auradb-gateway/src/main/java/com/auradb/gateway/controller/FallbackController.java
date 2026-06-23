package com.auradb.gateway.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/fallback")
public class FallbackController {

    @RequestMapping("/database")
    public ResponseEntity<Map<String, Object>> databaseFallback() {
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                .body(Map.of(
                        "error", "Service Unavailable",
                        "message", "The destination database is taking too long to respond. The Circuit Breaker has opened to protect the Gateway.",
                        "status", 503
                ));
    }
}
