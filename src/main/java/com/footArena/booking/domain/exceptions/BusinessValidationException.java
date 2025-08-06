package com.footArena.booking.domain.exceptions;

import java.util.List;
import java.util.Map;

public class BusinessValidationException extends BaseException {
    private final Map<String, List<String>> fieldErrors;

    public BusinessValidationException(String message) {
        super(message, "BUSINESS_VALIDATION_ERROR");
        this.fieldErrors = Map.of();
    }

    public BusinessValidationException(String message, Map<String, List<String>> fieldErrors) {
        super(message, "BUSINESS_VALIDATION_ERROR");
        this.fieldErrors = fieldErrors;
    }

    public Map<String, List<String>> getFieldErrors() {
        return fieldErrors;
    }
}