package com.gyote.silvercare.global.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class FrontendOrigin {

    private final String origin;

    public FrontendOrigin(@Value("${silvercare.frontend-origin:http://localhost:3000}") String origin) {
        String trimmed = origin == null ? "http://localhost:3000" : origin.trim();
        this.origin = trimmed.endsWith("/") ? trimmed.substring(0, trimmed.length() - 1) : trimmed;
    }

    public String get() {
        return origin;
    }

    public String path(String path) {
        if (path == null || path.isBlank()) {
            return origin + "/";
        }
        if ("/".equals(path)) {
            return origin + "/";
        }
        return origin + (path.startsWith("/") ? path : "/" + path);
    }
}
