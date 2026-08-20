package com.library.smart_internship.service;

import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class LoginAttemptService {

    private static final int MAX_ATTEMPTS = 5;
    private static final int LOCK_DURATION_MINUTES = 15;

    private final ConcurrentHashMap<String, Integer> attempts = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, LocalDateTime> lockedUntil = new ConcurrentHashMap<>();

    public void loginFailed(String email) {
        if (email == null) return;
        String key = email.toLowerCase();
        int count = attempts.merge(key, 1, Integer::sum);
        if (count >= MAX_ATTEMPTS) {
            lockedUntil.put(key, LocalDateTime.now().plusMinutes(LOCK_DURATION_MINUTES));
        }
    }

    public void loginSucceeded(String email) {
        if (email == null) return;
        String key = email.toLowerCase();
        attempts.remove(key);
        lockedUntil.remove(key);
    }

    public boolean isLocked(String email) {
        if (email == null) return false;
        String key = email.toLowerCase();
        LocalDateTime until = lockedUntil.get(key);
        if (until == null) return false;
        if (LocalDateTime.now().isAfter(until)) {
            lockedUntil.remove(key);
            attempts.remove(key);
            return false;
        }
        return true;
    }
}