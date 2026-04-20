package com.gomsu.workshopservice.controller;

import com.gomsu.workshopservice.dto.request.WorkshopRequest;
import com.gomsu.workshopservice.dto.request.WorkshopUpdateRequest;
import com.gomsu.workshopservice.dto.response.WorkshopAttendeeResponse;
import com.gomsu.workshopservice.dto.response.WorkshopResponse;
import com.gomsu.workshopservice.entity.RegistrationStatus;
import com.gomsu.workshopservice.service.WorkshopService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import java.time.LocalDateTime;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;


@RestController
@RequestMapping("/workshops")
@RequiredArgsConstructor
@Slf4j
@CrossOrigin("*")
public class WorkshopController {
    private final WorkshopService workshopService;

    @PostMapping(value = "/create", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasRole('ADMIN')") // Chặn User thường, chỉ cho phép ADMIN
    public ResponseEntity<WorkshopResponse> createWorkshop(
            @Valid @RequestPart("workshop") WorkshopRequest request, // Nhận dữ liệu JSON
            @RequestPart("images") List<MultipartFile> images // Nhận danh sách File ảnh
    ) {
        log.info("Admin đang tạo Workshop mới: {}", request.getName());
        WorkshopResponse response = workshopService.createWorkshop(request, images);
        return new ResponseEntity<>(response, HttpStatus.CREATED);
    }

    @GetMapping("/all")
    public ResponseEntity<Page<WorkshopResponse>> getAllWorkshops(
            @AuthenticationPrincipal Jwt jwt, // Lấy thông tin user từ token
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String location,
            @RequestParam(required = false) Double minPrice,
            @RequestParam(required = false) Double maxPrice,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime fromDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime toDate,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "startDate") String sortBy,
            @RequestParam(defaultValue = "DESC") String sortDir
    ) {
        // 1. Kiểm tra xem có phải Admin không (Dựa vào Roles trong Token của Nguyệt)
        // Giả sử claim roles của Nguyệt trả về list các String
        boolean isAdmin = false;

        if (jwt != null) {
            // 1. Lấy claim "scope" (vì Nguyệt đặt tên là scope trong token)
            String scope = jwt.getClaimAsString("scope");

            // 2. Kiểm tra xem trong chuỗi scope có chứa ROLE_ADMIN không
            if (scope != null && scope.contains("ROLE_ADMIN")) {
                isAdmin = true;
            }

            // Bonus: Tiện thể lấy luôn userId cho chuẩn bài
            Long userId = jwt.getClaim("userId");
            log.info("User ID {} đang truy cập với quyền Admin: {}", userId, isAdmin);
        }

        Page<WorkshopResponse> response = workshopService.getWorkshops(
                keyword, location, minPrice, maxPrice, fromDate, toDate, isAdmin, page, size, sortBy, sortDir);

        return ResponseEntity.ok(response);
    }

    @GetMapping("/{id}")
    public ResponseEntity<WorkshopResponse> getWorkshopById(@PathVariable Long id) {
        WorkshopResponse response = workshopService.getWorkshopById(id);
        return ResponseEntity.ok(response);
    }

    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping(value = "/{id}/add-images", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<WorkshopResponse> addImagesToWorkshop(
            @PathVariable Long id,
            @RequestPart("images") List<MultipartFile> images) {

        WorkshopResponse response = workshopService.uploadWorkshopImages(id, images);

        return ResponseEntity.ok(response);
    }

    @PreAuthorize("hasRole('ADMIN')")
    @PutMapping(value = "/{id}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<WorkshopResponse> updateWorkshop(
            @PathVariable Long id,
            @RequestPart("request") WorkshopUpdateRequest request, // Gửi JSON ở part này
            @RequestPart(value = "images", required = false) List<MultipartFile> images // Gửi file ở part này
    ) {
        log.info("Admin đang cập nhật Workshop ID: {}", id);
        return ResponseEntity.ok(workshopService.updateWorkshop(id, request, images));
    }

    // API Xóa Workshop
    @PreAuthorize("hasRole('ADMIN')")
    @DeleteMapping("/{id}")
    public ResponseEntity<String> deleteWorkshop(@PathVariable Long id) {
        log.info("Admin yêu cầu xóa Workshop ID: {}", id);
        workshopService.deleteWorkshop(id);
        return ResponseEntity.ok("Xóa workshop thành công!");
    }

    @GetMapping("/{id}/attendees")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<WorkshopAttendeeResponse> getAttendees(
            @PathVariable Long id,
            @RequestParam(required = false) RegistrationStatus status,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime fromDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime toDate,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "registrationDate") String sortBy,
            @RequestParam(defaultValue = "DESC") String sortDir
    ) {
        log.info("Admin truy cập danh sách người tham gia Workshop ID: {}, Page: {}", id, page);

        WorkshopAttendeeResponse response = workshopService.getAttendeesWithFilter(
                id, status, keyword, fromDate, toDate, page, size, sortBy, sortDir);

        return ResponseEntity.ok(response);
    }

}
