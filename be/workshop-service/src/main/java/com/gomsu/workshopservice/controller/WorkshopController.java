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
import java.util.Map;


@RestController
@RequestMapping("/workshops")
@RequiredArgsConstructor
@Slf4j
public class WorkshopController {
    private final WorkshopService workshopService;

    // THÀNH CÔNG
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

    // THÀNH CÔNG
    @GetMapping("/all")
    public ResponseEntity<Page<WorkshopResponse>> getAllWorkshops(
            @AuthenticationPrincipal Jwt jwt,
            @RequestParam(required = false) Boolean active, // 1. Bổ sung nhận diện lọc trạng thái
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
        Boolean isAdmin = false;

        if (jwt != null) {
            String scope = jwt.getClaimAsString("scope");
            log.info(">>> Check quyền truy cập - Scope nhận được: [{}]", scope);

            // Vẫn dùng logic chứa chữ ADMIN để linh hoạt
            if (scope != null && scope.contains("ADMIN")) {
                isAdmin = true;
            }

            Long userId = Long.valueOf(jwt.getClaim("userId").toString());
            log.info(">>> User ID: {} | Quyền Admin: {}", userId, isAdmin);
        }

        // 2. Truyền thêm biến active vào hàm gọi Service
        Page<WorkshopResponse> response = workshopService.getWorkshops(
                keyword, location, minPrice, maxPrice, fromDate, toDate,
                isAdmin,
                active, // <-- Nhớ truyền thêm tham số này vào Hiệu nhé
                page, size, sortBy, sortDir);

        return ResponseEntity.ok(response);
    }

    // THÀNH CÔNG
    @GetMapping("/{id}")
    public ResponseEntity<WorkshopResponse> getWorkshopById(@PathVariable Long id) {
        WorkshopResponse response = workshopService.getWorkshopById(id);
        return ResponseEntity.ok(response);
    }

    // THÀNH CÔNG
    @GetMapping("/s/{slug}")
    public ResponseEntity<WorkshopResponse> getWorkshopBySlug(@PathVariable String slug) {
        WorkshopResponse response = workshopService.getWorkshopBySlug(slug);
        return ResponseEntity.ok(response);
    }

    // THÀNH CÔNG
    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping(value = "/{id}/add-images", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<WorkshopResponse> addImagesToWorkshop(
            @PathVariable Long id,
            @RequestPart("images") List<MultipartFile> images) {

        WorkshopResponse response = workshopService.uploadWorkshopImages(id, images);

        return ResponseEntity.ok(response);
    }

    // THÀNH CÔNG
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
    // THÀNH CÔNG
    @PreAuthorize("hasRole('ADMIN')")
    @DeleteMapping("/{id}")
    public ResponseEntity<String> deleteWorkshop(@PathVariable Long id) {
        log.info("Admin yêu cầu xóa Workshop ID: {}", id);
        workshopService.deleteWorkshop(id);
        return ResponseEntity.ok("Xóa workshop thành công!");
    }

    // THÀNH CÔNG
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

    // THÀNH CÔNG
    @GetMapping("/{id}/stats")
    @PreAuthorize("hasRole('ADMIN')") // Chốt chặn bảo mật: Chỉ Admin mới được xem tiền
    public ResponseEntity<Map<String, Object>> getWorkshopStats(@PathVariable Long id) {
        // Gọi sang Service để lấy Map chứa totalTickets và totalRevenue
        Map<String, Object> stats = workshopService.getWorkshopStatsByWorkshopID(id);

        // Nếu chưa có ai mua vé, SUM sẽ trả về NULL, mình nên xử lý một chút cho đẹp
        if (stats.get("totalTickets") == null) {
            stats.put("totalTickets", 0);
            stats.put("totalRevenue", 0);
        }

        return ResponseEntity.ok(stats);
    }

    // THÀNH CÔNG
    @GetMapping("/statistics")
    @PreAuthorize("hasRole('ADMIN')") // Chỉ Admin mới được xem "ví tiền" của hệ thống
    public ResponseEntity<List<Map<String, Object>>> getAllWorkshopsStatistics() {
        // Gọi Service để lấy danh sách (bao gồm cả các Workshop có doanh thu = 0)
        List<Map<String, Object>> stats = workshopService.getAllWorkshopsStatistics();

        // Trả về kết quả kèm trạng thái 200 OK
        return ResponseEntity.ok(stats);
    }

}
