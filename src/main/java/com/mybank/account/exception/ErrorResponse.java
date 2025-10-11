package com.mybank.account.exception;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;

import java.time.Instant;

/**
 * error formatter
 */
@Data
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ErrorResponse {
    private String errorCode;
    private String message;
    private String path;

    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", timezone = "UTC")
    private Instant timestamp;

    // 静态工厂方法
    public static ErrorResponse of(String errorCode, String message) {
        ErrorResponse response = new ErrorResponse();
        response.errorCode = errorCode;
        response.message = message;
        response.timestamp = Instant.now();
        return response;
    }

    public static ErrorResponse of(String errorCode, String message, String path) {
        ErrorResponse response = of(errorCode, message);
        response.path = path;
        return response;
    }

    public ErrorResponse() {
        this.timestamp = Instant.now();
    }
}