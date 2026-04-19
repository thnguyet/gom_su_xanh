package com.gomsu.workshopservice.controller;

import com.gomsu.workshopservice.dto.response.RegistrationResponse;
import com.gomsu.workshopservice.entity.RegistrationStatus;
import com.gomsu.workshopservice.service.RegistrationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;

@RestController
@RequestMapping("/regis-workshops")
@RequiredArgsConstructor
@Slf4j
@CrossOrigin("*")
public class RegistrationController {
    private final RegistrationService registerWorkshop;
    @PostMapping("/register")
    public ResponseEntity<RegistrationResponse> register(
            @AuthenticationPrincipal Jwt jwt, // Lấy trực tiếp từ Token JWT
            @RequestParam Long workshopId,
            @RequestParam Integer quantity,
            @RequestParam(required = false) String note) {

        // Giả sử trong Token Nguyệt lưu userId vào claim tên là "id"
        Long userId = jwt.getClaim("userId");

        RegistrationResponse response = registerWorkshop.registerWorkshop(userId, workshopId, quantity, note);
        return new ResponseEntity<>(response, HttpStatus.CREATED);
    }
    @GetMapping("/my-registrations")
    public ResponseEntity<Page<RegistrationResponse>> getMyRegistrations(
            @AuthenticationPrincipal Jwt jwt,
            @RequestParam(required = false) RegistrationStatus status,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime fromDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime toDate,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "registrationDate") String sortBy,
            @RequestParam(defaultValue = "DESC") String sortDir) {

        // Lấy userId tự động từ Token
        Long userId = jwt.getClaim("userId");
        log.info("User {} truy cập lịch sử đặt vé", userId);

        Page<RegistrationResponse> response = registerWorkshop.getMyRegistrations(
                userId, status, keyword, fromDate, toDate, page, size, sortBy, sortDir);

        return ResponseEntity.ok(response);
    }
}
