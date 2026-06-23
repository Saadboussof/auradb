package com.auradb.gateway.controller;

import com.auradb.gateway.security.JwtUtil;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final JwtUtil jwtUtil;

    public AuthController(JwtUtil jwtUtil) {
        this.jwtUtil = jwtUtil;
    }

    @GetMapping("/token")
    public ResponseEntity<Map<String, String>> generateToken(@RequestParam String tenantId) {
        String token = jwtUtil.generateToken(tenantId);
        return ResponseEntity.ok(Map.of("token", token));
    }
}
