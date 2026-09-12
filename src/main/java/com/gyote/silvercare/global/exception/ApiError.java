package com.gyote.silvercare.global.exception;

public record ApiError(String error) {

    public static ApiError of(String message) {
        if (message != null && message.contains("없는 코드")) {
            return new ApiError("code");
        }
        if (message != null && message.contains("자기 자신")) {
            return new ApiError("self");
        }
        if (message != null && message.contains("이미")) {
            return new ApiError("duplicate");
        }
        return new ApiError("fail");
    }
}
