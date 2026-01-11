package com.neurogine.wallet.controller;

import com.neurogine.wallet.dto.JwtRequest;
import com.neurogine.wallet.dto.JwtResponse;
import com.neurogine.wallet.entity.User;
import com.neurogine.wallet.security.JwtUtil;
import com.neurogine.wallet.service.AuthService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * Authentication Controller
 * Handles user registration and login with JWT token generation.
 * Demonstrates REST API design, validation, and security best practices.
 */
@RestController
@RequestMapping("/api/auth")
@CrossOrigin(origins = "http://localhost:5173")
public class AuthController {

    @Autowired
    private AuthService authService;

    @Autowired
    private JwtUtil jwtUtil;

    /**
     * Register a new user
     * 
     * @param request JwtRequest with username and password
     * @return Success message with user info
     */
    @PostMapping("/register")
    public ResponseEntity<?> register(@Valid @RequestBody JwtRequest request) {
        User registered = authService.registerUser(request.getUsername(), request.getPassword());
        return ResponseEntity.ok().body(new java.util.HashMap<String, Object>() {
            {
                put("message", "User registered successfully");
                put("userId", registered.getId());
                put("username", registered.getUsername());
            }
        });
    }

    /**
     * Login user and return JWT token
     * 
     * @param request JwtRequest with username and password
     * @return JwtResponse with token and user info
     */
    @PostMapping("/login")
    public ResponseEntity<JwtResponse> login(@Valid @RequestBody JwtRequest request) {
        User loggedIn = authService.login(request.getUsername(), request.getPassword());

        // Generate JWT token
        String token = jwtUtil.generateToken(loggedIn.getUsername());

        // Return token with user info
        JwtResponse response = new JwtResponse(token, loggedIn.getUsername(), loggedIn.getId());
        return ResponseEntity.ok(response);
    }
}
