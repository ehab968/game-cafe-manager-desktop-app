package com.gamecafe.gamecafemanager.domain.service;

import com.gamecafe.gamecafemanager.core.validation.ValidationException;
import com.gamecafe.gamecafemanager.domain.model.Role;
import java.util.LinkedHashMap;
import java.util.Map;

public final class UserValidator {

    private static final int MINIMUM_USERNAME_LENGTH = 3;
    private static final int MAXIMUM_USERNAME_LENGTH = 50;
    private static final int MINIMUM_PASSWORD_LENGTH = 8;

    public void validate(String username, Role role, char[] password) {
        Map<String, String> errors = new LinkedHashMap<>();
        validateUsername(username, errors);
        if (role == null) {
            errors.put("role", "Role is required");
        }
        validatePassword(password, errors);
        throwIfInvalid(errors);
    }

    public void validatePassword(char[] password) {
        Map<String, String> errors = new LinkedHashMap<>();
        validatePassword(password, errors);
        throwIfInvalid(errors);
    }

    public String normalizeUsername(String username) {
        return username.trim();
    }

    private void validateUsername(String username, Map<String, String> errors) {
        String normalized = username == null ? "" : username.trim();
        if (normalized.length() < MINIMUM_USERNAME_LENGTH) {
            errors.put("username", "Username must contain at least 3 characters");
        } else if (normalized.length() > MAXIMUM_USERNAME_LENGTH) {
            errors.put("username", "Username must not exceed 50 characters");
        }
    }

    private void validatePassword(char[] password, Map<String, String> errors) {
        if (password == null || password.length < MINIMUM_PASSWORD_LENGTH) {
            errors.put("password", "Password must contain at least 8 characters");
        }
    }

    private void throwIfInvalid(Map<String, String> errors) {
        if (!errors.isEmpty()) {
            throw new ValidationException(errors);
        }
    }
}
