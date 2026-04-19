package com.gomsu.workshopservice.controller;

import com.gomsu.workshopservice.dto.request.WorkshopRequest;
import com.gomsu.workshopservice.dto.response.WorkshopResponse;
import com.gomsu.workshopservice.repository.WorkshopRepository;
import com.gomsu.workshopservice.service.WorkshopService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
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
}
