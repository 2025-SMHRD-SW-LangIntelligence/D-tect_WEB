package com.smhrd.dtect.advice;

import java.util.Map;

import org.hibernate.exception.ConstraintViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.validation.BindException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.multipart.MultipartException;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.context.request.ServletWebRequest;

@RestControllerAdvice
public class ApiExceptionHandler {

    @ExceptionHandler(ResponseStatusException.class)
    public ResponseEntity<Map<String, String>> handleRSE(ResponseStatusException e) {
        return ResponseEntity
                .status(e.getStatusCode())
                .body(Map.of("message", String.valueOf(e.getReason())));
    }

    // 요청 본문 파싱/검증 오류
    @ExceptionHandler({ MethodArgumentNotValidException.class, BindException.class, HttpMessageNotReadableException.class })
    public ResponseEntity<Map<String, String>> handleValidation(Exception e) {
        String msg = "요청 형식이 올바르지 않습니다.";
        if (e instanceof MethodArgumentNotValidException manv && manv.getBindingResult().hasErrors()) {
            msg = manv.getBindingResult().getAllErrors().get(0).getDefaultMessage();
        } else if (e instanceof BindException be && be.getBindingResult().hasErrors()) {
            msg = be.getBindingResult().getAllErrors().get(0).getDefaultMessage();
        }
        return ResponseEntity.badRequest().body(Map.of("message", msg));
    }

    // 비즈니스 검증 실패
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Map<String, String>> handleIllegalArgument(IllegalArgumentException e) {
        return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
    }

    @ExceptionHandler(IllegalStateException.class)
    public ResponseEntity<Map<String, String>> handleIllegalState(IllegalStateException e) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("message", e.getMessage()));
    }

    // 기타 예외 (SSE 요청은 JSON 응답 없이 스트림 종료)
    @ExceptionHandler(Exception.class)
    public ResponseEntity<?> handleAny(Exception e, WebRequest request) {
        if (request instanceof ServletWebRequest swr) {
            String accept = swr.getRequest().getHeader("Accept");
            String uri = swr.getRequest().getRequestURI();
            if ((accept != null && accept.contains("text/event-stream")) || (uri != null && uri.contains("/events"))) {
                return ResponseEntity.noContent().build(); // 204, 바디 없음
            }
        }
        // log.error("Unhandled", e);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of("message", "서버 오류가 발생했습니다."));
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<Map<String, String>> handleAccessDenied(AccessDeniedException e) {
        String msg = (e.getMessage() == null || e.getMessage().isBlank())
                ? "접근 권한이 없습니다."
                : e.getMessage();
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("message", msg));
    }

    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public ResponseEntity<Map<String, String>> handleMaxUpload(MaxUploadSizeExceededException e) {
        return ResponseEntity.status(HttpStatus.PAYLOAD_TOO_LARGE)
                .body(Map.of("message", "파일이 너무 큽니다"));
    }

    @ExceptionHandler(MultipartException.class)
    public ResponseEntity<Map<String, String>> handleMultipart(MultipartException e) {
        return ResponseEntity.badRequest()
                .body(Map.of("message", "멀티파트 요청을 확인해 주세요"));
    }

    @ExceptionHandler({
            MissingServletRequestParameterException.class,
            MethodArgumentTypeMismatchException.class,
            ConstraintViolationException.class
    })
    public ResponseEntity<Map<String, String>> handleParams(Exception e) {
        String msg = "요청 파라미터가 올바르지 않습니다.";
        if (e instanceof MissingServletRequestParameterException m) {
            msg = "필수 파라미터가 없습니다: " + m.getParameterName();
        }
        return ResponseEntity.badRequest().body(Map.of("message", msg));
    }
}
