package com.lastkey.backend.common;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/")
public class RootController {

    @GetMapping
    public ResponseEntity<Map<String, String>> healthCheck() {
        return ResponseEntity.ok(Map.of(
            "status", "UP",
            "message", "LastKey Backend API is Running Successfully!"
        ));
    }
}