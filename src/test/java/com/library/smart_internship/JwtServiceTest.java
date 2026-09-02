package com.library.smart_internship.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class JwtServiceTest {

    private static final String TEST_SECRET = "WmI16cza7/gr21fDB8tn55Q/oy8rKh4IfYJqNdbCurM=";

    private JwtService jwtService;

    @BeforeEach
    void setUp() {
        jwtService = new JwtService();
        ReflectionTestUtils.setField(jwtService, "jwtSecret", TEST_SECRET);
        ReflectionTestUtils.setField(jwtService, "jwtExpiration", 86400000L);
    }

    @Test
    void generatedTokenContainsSubjectEmail() {
        String token = jwtService.generateToken("student@example.com", List.of("ROLE_STUDENT"));

        assertThat(jwtService.extractEmail(token)).isEqualTo("student@example.com");
    }

    @Test
    void generatedTokenIsValidForMatchingEmail() {
        String token = jwtService.generateToken("student@example.com", List.of("ROLE_STUDENT"));

        assertThat(jwtService.isTokenValid(token, "student@example.com")).isTrue();
    }

    @Test
    void tokenIsInvalidForDifferentEmail() {
        String token = jwtService.generateToken("student@example.com", List.of("ROLE_STUDENT"));

        assertThat(jwtService.isTokenValid(token, "other@example.com")).isFalse();
    }

    @Test
    void tokenIsInvalidWhenExpired() {
        ReflectionTestUtils.setField(jwtService, "jwtExpiration", -1000L);

        String token = jwtService.generateToken("student@example.com", List.of("ROLE_STUDENT"));

        assertThat(jwtService.isTokenValid(token, "student@example.com")).isFalse();
    }

    @Test
    void malformedTokenIsNeverValid() {
        assertThat(jwtService.isTokenValid("not-a-real-token", "student@example.com")).isFalse();
    }

    @Test
    void extractedClaimsContainRoles() {
        String token = jwtService.generateToken("recruiter@example.com", List.of("ROLE_RECRUITER"));

        Object roles = jwtService.extractAllClaims(token).get("roles");

        assertThat(roles).isEqualTo(List.of("ROLE_RECRUITER"));
    }
}