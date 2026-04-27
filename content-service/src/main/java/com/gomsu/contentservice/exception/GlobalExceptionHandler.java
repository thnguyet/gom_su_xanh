package com.gomsu.contentservice.exception;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.stream.Collectors;

@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

    @ExceptionHandler(RuntimeException.class)
    public ResponseEntity<Map<String, Object>> handleRuntimeException(RuntimeException e) {
        // Log lỗi ra console để Admin dễ theo dõi
        log.error("Business Logic Error: {}", e.getMessage());

        Map<String, Object> body = new LinkedHashMap<>();
        body.put("timestamp", LocalDateTime.now());
        body.put("status", HttpStatus.BAD_REQUEST.value()); // Mã 400
        body.put("error", "Bad Request");
        body.put("message", e.getMessage()); // Câu thông báo tiếng Việt từ Service

        return new ResponseEntity<>(body, HttpStatus.BAD_REQUEST);
    }

    // Bonus: Bắt các lỗi hệ thống không mong muốn khác (Lỗi 500)
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, Object>> handleValidationException(MethodArgumentNotValidException ex) {
        // 1. Lấy ra thông báo lỗi đầu tiên mà mình đã định nghĩa ở DTO
        String errorMessage = ex.getBindingResult().getFieldErrors()
                .stream()
                .map(fieldError -> fieldError.getDefaultMessage())
                .collect(Collectors.joining(", "));

        // 2. Tạo body trả về
        Map<String, Object> body = new HashMap<>();
        body.put("timestamp", LocalDateTime.now());
        body.put("status", HttpStatus.BAD_REQUEST.value()); // Trả về 400 thay vì 500
        body.put("error", "Validation Error");
        body.put("message", errorMessage); // Đây chính là chỗ Nguyệt muốn hiện lỗi!

        return new ResponseEntity<>(body, HttpStatus.BAD_REQUEST);
    }
}