package com.diluwar.auth.service;

import io.jsonwebtoken.JwtException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class JwtServiceTest {

    private JwtService jwtService;

    @BeforeEach
    void setUp() {
        jwtService = new JwtService();
    }

    @Test
    void shouldGenerateValidToken() {
        String email = "john.doe@example.com";
        String role = "USER";

        String token = jwtService.generateToken(email, role);

        assertNotNull(token);
        assertFalse(token.isBlank());
    }

    @Test
    void shouldExtractEmailFromToken() {
        String email = "jane.doe@example.com";
        String role = "ADMIN";

        String token = jwtService.generateToken(email, role);
        String extractedEmail = jwtService.extractEmail(token);

        assertEquals(email, extractedEmail);
    }

    @Test
    void shouldExtractRoleFromToken() {
        String email = "admin@example.com";
        String role = "ADMIN";

        String token = jwtService.generateToken(email, role);
        String extractedRole = jwtService.extractRole(token);

        assertEquals(role, extractedRole);
    }

    @Test
    void shouldValidateTokenForCorrectEmail() {
        String email = "user@example.com";
        String role = "USER";

        String token = jwtService.generateToken(email, role);

        assertTrue(jwtService.isTokenValid(token, email));
        assertFalse(jwtService.isTokenValid(token, "other@example.com"));
    }

    @Test
    void shouldFailWhenTokenIsTampered() {
        String token = jwtService.generateToken("user@example.com", "USER");
        String tamperedToken = token + "corrupted";

        assertThrows(JwtException.class, () -> jwtService.extractEmail(tamperedToken));
    }
}
