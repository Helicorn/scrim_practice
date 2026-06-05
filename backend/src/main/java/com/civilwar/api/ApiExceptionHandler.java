package com.civilwar.api;

import java.util.Map;

import org.springframework.dao.DataAccessException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class ApiExceptionHandler {

	@ExceptionHandler(ApiException.class)
	public ResponseEntity<Map<String, String>> handleApiException(ApiException ex) {
		return ResponseEntity.status(ex.getStatus()).body(Map.of("message", ex.getMessage()));
	}

	@ExceptionHandler(DataAccessException.class)
	public ResponseEntity<Map<String, String>> handleDataAccess(DataAccessException ex) {
		String raw = ex.getMostSpecificCause() != null
				? ex.getMostSpecificCause().getMessage()
				: ex.getMessage();
		if (raw != null && raw.contains("ORA-01950")) {
			return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
					.body(Map.of(
							"message",
							"Oracle 테이블스페이스 쿼터가 없습니다. SYS 계정으로 "
									+ "database/grant-tablespace-quota.sql 을 실행한 뒤 다시 시도해 주세요."));
		}
		return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
				.body(Map.of("message", raw != null ? raw : "데이터베이스 오류가 발생했습니다."));
	}

	@ExceptionHandler(MethodArgumentNotValidException.class)
	public ResponseEntity<Map<String, String>> handleValidation(MethodArgumentNotValidException ex) {
		String message = ex.getBindingResult().getFieldErrors().stream()
				.findFirst()
				.map(err -> err.getField() + ": " + err.getDefaultMessage())
				.orElse("요청 값이 올바르지 않습니다.");
		return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("message", message));
	}
}
