package com.smhrd.dtect.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.server.ResponseStatusException;

import java.util.HashMap;
import java.util.Map;

@RestControllerAdvice
public class GlobalExceptionHandler {

    // ResponseStatusException -> { "message": "...", "status": 400 } 형태 등으로 내려주기
    @ExceptionHandler(ResponseStatusException.class)
    public ResponseEntity<Map<String, Object>> handleRSE(ResponseStatusException e) {
        Map<String, Object> body = new HashMap<>();
        body.put("message", e.getReason());
        body.put("status", e.getStatusCode().value());
        return ResponseEntity.status(e.getStatusCode()).body(body);
    }

    // 일반 예외(필요 시)
    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, Object>> handleGeneric(Exception e) {
        Map<String, Object> body = new HashMap<>();
        body.put("message", e.getMessage());
        return ResponseEntity.internalServerError().body(body);
    }
}