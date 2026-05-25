package com.minimarket.security.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class LoginAttemptService {

    private static final Logger log = LoggerFactory.getLogger(LoginAttemptService.class);

    private final int maxAttempts;
    private final Map<String, Integer> failedAttempts = new ConcurrentHashMap<>();

    public LoginAttemptService(@Value("${security.login.max-attempts:3}") int maxAttempts) {
        this.maxAttempts = maxAttempts;
    }

    public void loginFailed(String username) {
        if (username == null || username.isBlank()) {
            return;
        }
        int attempts = failedAttempts.merge(username, 1, Integer::sum);
        log.warn("Intento de login fallido para '{}'. Intentos: {}/{}", username, attempts, maxAttempts);
        if (attempts >= maxAttempts) {
            log.warn("Cuenta temporalmente bloqueada por intentos fallidos: {}", username);
        }
    }

    public void loginSucceeded(String username) {
        if (username != null) {
            failedAttempts.remove(username);
        }
    }

    public boolean isBlocked(String username) {
        return failedAttempts.getOrDefault(username, 0) >= maxAttempts;
    }
}
