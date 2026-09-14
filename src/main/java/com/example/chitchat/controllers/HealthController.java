package com.example.chitchat.controllers;

import com.example.chitchat.config.RequestTrackingFilter;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
public class HealthController {

    private final RequestTrackingFilter requestTrackingFilter;

    public HealthController(RequestTrackingFilter requestTrackingFilter) {
        this.requestTrackingFilter = requestTrackingFilter;
    }

    @GetMapping("/health")
    public ResponseEntity<Map<String, Object>> health() {
        return ResponseEntity.ok(Map.of(
                "status", "UP",
                "active_connections", requestTrackingFilter.getActiveRequests()));
    }
}
