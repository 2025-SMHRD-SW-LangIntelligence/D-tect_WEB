package com.smhrd.dtect.controller;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.ServletWebRequest;
import org.springframework.web.context.request.WebRequest;

import java.util.HashMap;
import java.util.Map;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, Object>> handleGeneric(Exception e, WebRequest request) {
        if (isSseRequest(request)) {
            return ResponseEntity.noContent().build();
        }

        Map<String, Object> body = new HashMap<>();
        body.put("message", e.getMessage());
        return ResponseEntity.internalServerError().body(body);
    }

    private boolean isSseRequest(WebRequest request) {
        if (request instanceof ServletWebRequest swr) {
            HttpServletRequest req = swr.getRequest();
            String accept = req.getHeader("Accept");
            if (accept != null && accept.contains("text/event-stream")) {
                return true;
            }
            String path = req.getRequestURI();
            return path != null && path.contains("/events");
        }
        return false;
    }
}
