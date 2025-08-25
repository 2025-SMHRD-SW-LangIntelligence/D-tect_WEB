package com.smhrd.dtect.advice;

import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.BindException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.server.ResponseStatusException;

@RestControllerAdvice
public class ApiExceptionHandler {
	
	
	/**
	 * ResponseStatusException 전용 예외 처리기.
	 * - 이 메서드가 선언된 클래스(@RestController / @Controller)에서 발생한
	 *   ResponseStatusException을 가로채 동일한 HTTP 상태코드와 간단한 JSON 본문으로 응답한다.
	 *   (만약 @RestControllerAdvice/@ControllerAdvice 클래스에 있으면 전역 처리)
	 *
	 * @param e 처리 대상 예외(상태코드와 사유 메시지를 포함)
	 * @return 예외의 상태코드를 그대로 사용하고, {"message": "<reason>"} 형태의 본문을 담은 응답
	 */
	@ExceptionHandler(ResponseStatusException.class)
	public ResponseEntity<Map<String, String>> handleRSE(ResponseStatusException e) {
	    return ResponseEntity
	        // 예외에 들어있는 상태코드 그대로 사용(예: 400, 404, 409, 500 등)
	        .status(e.getStatusCode())
	        // 응답 본문: 단일 필드 "message"를 가진 불변 Map(Java 9+의 Map.of)
	        // String.valueOf(...)는 e.getReason()이 null이어도 NPE 없이 "null" 문자열을 넣어준다.
	        .body(Map.of("message", String.valueOf(e.getReason())));
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
        return ResponseEntity.badRequest().body(Map.of("message", msg));
    }
    
    // 비즈니스 검증 실패
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Map<String, String>> handleIllegalArgument(IllegalArgumentException e) {
        return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
    }
    
    // 중복/상태 충돌 오류
    @ExceptionHandler(IllegalStateException.class)
    public ResponseEntity<Map<String, String>> handleIllegalState(IllegalStateException e) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("message", e.getMessage()));
    }
    
    // 최종 방어선
    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, String>> handleAny(Exception e) {
        // log.error("Unhandled", e);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
            .body(Map.of("message", "서버 오류가 발생했습니다."));
    }
}
