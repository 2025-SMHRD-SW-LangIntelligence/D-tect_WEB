package com.smhrd.dtect.advice;

import com.smhrd.dtect.dto.ErrorResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.BindException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.server.ResponseStatusException;

@RestControllerAdvice
public class GlobalExceptionHandler {

    // ✅ ResponseStatusException 전용
    @ExceptionHandler(ResponseStatusException.class)
    public ResponseEntity<ErrorResponse> handleRSE(ResponseStatusException e) {
        return ResponseEntity.status(e.getStatusCode())
                .body(new ErrorResponse(
                        e.getReason() != null ? e.getReason() : "요청 오류",
                        e.getStatusCode().value()
                ));
    }

    // ✅ 요청 본문 파싱/검증 오류
    @ExceptionHandler({ MethodArgumentNotValidException.class, BindException.class, HttpMessageNotReadableException.class })
    public ResponseEntity<ErrorResponse> handleValidation(Exception e) {
        String msg = "요청 형식이 올바르지 않습니다.";
        if (e instanceof MethodArgumentNotValidException manv && manv.getBindingResult().hasErrors()) {
            msg = manv.getBindingResult().getAllErrors().get(0).getDefaultMessage();
        } else if (e instanceof BindException be && be.getBindingResult().hasErrors()) {
            msg = be.getBindingResult().getAllErrors().get(0).getDefaultMessage();
        }
        return ResponseEntity.badRequest()
                .body(new ErrorResponse(msg, HttpStatus.BAD_REQUEST.value()));
    }

    // ✅ 비즈니스 검증 실패
    @ExceptionHandler({ IllegalArgumentException.class, IllegalStateException.class })
    public ResponseEntity<ErrorResponse> handleBusiness(Exception e) {
        return ResponseEntity.badRequest()
                .body(new ErrorResponse(e.getMessage(), HttpStatus.BAD_REQUEST.value()));
    }

    // ✅ 최종 방어선
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleAny(Exception e) {
        // log.error("Unhandled exception", e);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(new ErrorResponse("서버 오류가 발생했습니다.", HttpStatus.INTERNAL_SERVER_ERROR.value()));
    }
}
