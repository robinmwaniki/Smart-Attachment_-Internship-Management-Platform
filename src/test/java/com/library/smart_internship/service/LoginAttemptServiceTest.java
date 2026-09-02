package com.library.smart_internship.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class LoginAttemptServiceTest {

    private LoginAttemptService loginAttemptService;

    @BeforeEach
    void setUp() {
        loginAttemptService = new LoginAttemptService();
    }

    @Test
    void accountIsNotLockedByDefault() {
        assertThat(loginAttemptService.isLocked("student@example.com")).isFalse();
    }

    @Test
    void accountLocksAfterMaxFailedAttempts() {
        String email = "student@example.com";

        for (int i = 0; i < 5; i++) {
            loginAttemptService.loginFailed(email);
        }

        assertThat(loginAttemptService.isLocked(email)).isTrue();
    }

    @Test
    void accountStaysUnlockedBelowThreshold() {
        String email = "student@example.com";

        for (int i = 0; i < 4; i++) {
            loginAttemptService.loginFailed(email);
        }

        assertThat(loginAttemptService.isLocked(email)).isFalse();
    }

    @Test
    void successfulLoginResetsFailedAttempts() {
        String email = "student@example.com";

        for (int i = 0; i < 4; i++) {
            loginAttemptService.loginFailed(email);
        }
        loginAttemptService.loginSucceeded(email);
        loginAttemptService.loginFailed(email);

        assertThat(loginAttemptService.isLocked(email)).isFalse();
    }

    @Test
    void emailMatchingIsCaseInsensitive() {
        String email = "Student@Example.com";

        for (int i = 0; i < 5; i++) {
            loginAttemptService.loginFailed(email);
        }

        assertThat(loginAttemptService.isLocked("student@example.com")).isTrue();
    }

    @Test
    void nullEmailIsNeverLocked() {
        loginAttemptService.loginFailed(null);

        assertThat(loginAttemptService.isLocked(null)).isFalse();
    }
}