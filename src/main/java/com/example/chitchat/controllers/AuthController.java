package com.example.chitchat.controllers;

import com.example.chitchat.dto.AuthRequest;
import com.example.chitchat.dto.AuthResponse;
import com.example.chitchat.service.AuthService;
import com.nimbusds.jose.JOSEException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.text.ParseException;

@RestController
public class AuthController {
    private final AuthService authService;

    public AuthController(AuthService authService){
        this.authService = authService;
    }

    @PostMapping("/auth/login")
    public ResponseEntity<AuthResponse> authorizeUser(@RequestBody AuthRequest req){
        AuthResponse response = authService.authorizeUser(req);
        if (response == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        return ResponseEntity.ok(response);
    }

    @PostMapping("/auth/refresh")
    public ResponseEntity<AuthResponse> refreshUserToken(
            @RequestBody String refreshToken)
            throws ParseException, JOSEException {

        AuthResponse newAccessToken =
                authService.refreshUserToken(refreshToken);

        if (newAccessToken == null) {
            return ResponseEntity
                    .status(HttpStatus.UNAUTHORIZED)
                    .build();
        }

        return ResponseEntity.ok(newAccessToken);
    }


}
