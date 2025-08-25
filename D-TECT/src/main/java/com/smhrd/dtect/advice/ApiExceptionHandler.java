package com.smhrd.dtect.advice;

import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.BindException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * REST 전역 예외 처리기.
 * /api/** 같은 JSON API에서 던진 예외를 일관된 형태로 내려줍니다.
 */
@RestControllerAdvice
public class ApiExceptionHandler {

    // 비즈니스 검증 실패
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Map<String, String>> handleIllegalArgument(IllegalArgumentException e) {
        return ResponseEntity.badRequest().body(Map.of(
            "field", "",
            "message", e.getMessage()
        ));
    }

    // 중복/상태 충돌 등
    @ExceptionHandler(IllegalStateException.class)
    public ResponseEntity<Map<String, String>> handleIllegalState(IllegalStateException e) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of(
            "field", "",
            "message", e.getMessage()
        ));
        // 필요하면 CONFLICT(409)로 바꾸세요.
    }

    // 요청 본문 파싱/검증 오류(선택)
    @ExceptionHandler({ MethodArgumentNotValidException.class, BindException.class, HttpMessageNotReadableException.class })
    public ResponseEntity<Map<String, String>> handleValidation(Exception e) {
        String msg = "요청 형식이 올바르지 않습니다.";
        if (e instanceof MethodArgumentNotValidException manv && manv.getBindingResult().hasErrors()) {
            msg = manv.getBindingResult().getAllErrors().get(0).getDefaultMessage();
        } else if (e instanceof BindException be && be.getBindingResult().hasErrors()) {
            msg = be.getBindingResult().getAllErrors().get(0).getDefaultMessage();
        }
        return ResponseEntity.badRequest().body(Map.of("field", "", "message", msg));
    }

    // 최종 방어선
    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, String>> handleAny(Exception e) {
        // 운영이라면 로깅 추가 권장: log.error("Unhandled", e);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of(
            "field", "",
            "message", "서버 오류가 발생했습니다."
        ));
    }
}
