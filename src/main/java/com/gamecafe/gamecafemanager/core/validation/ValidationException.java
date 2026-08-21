package com.gamecafe.gamecafemanager.core.validation;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Contains field-level validation failures suitable for presentation by any UI.
 */
public class ValidationException extends RuntimeException {

    private final Map<String, String> errors;

    public ValidationException(Map<String, String> errors) {
        super("Validation failed");
        this.errors = Collections.unmodifiableMap(new LinkedHashMap<>(errors));
    }

    public static ValidationException forField(String field, String message) {
        Map<String, String> errors = new LinkedHashMap<>();
        errors.put(field, message);
        return new ValidationException(errors);
    }

    public Map<String, String> getErrors() {
        return errors;
    }
}
