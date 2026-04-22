package com.gomsu.workshopservice.exception;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;

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
    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, Object>> handleGeneralException(Exception e) {
        log.error("System Error: ", e);

        Map<String, Object> body = new LinkedHashMap<>();
        body.put("timestamp", LocalDateTime.now());
        body.put("status", HttpStatus.INTERNAL_SERVER_ERROR.value()); // Mã 500
        body.put("error", "Internal Server Error");
        body.put("message", "Có lỗi hệ thống xảy ra, vui lòng thử lại sau!");

        return new ResponseEntity<>(body, HttpStatus.INTERNAL_SERVER_ERROR);
    }
}