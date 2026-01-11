package com.neurogine.wallet.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

/**
 * JWT Response DTO containing token and user information
 */
@Data
@AllArgsConstructor
public class JwtResponse {
    private String token;
    private String username;
    private Long userId;
}
