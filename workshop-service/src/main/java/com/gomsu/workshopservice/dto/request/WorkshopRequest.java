package com.gomsu.workshopservice.dto.request;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WorkshopRequest {

    @NotBlank(message = "Tên workshop không được để trống")
    private String name;

    @NotBlank(message = "Mô tả ngắn không được để trống")
    private String description;

    @NotBlank(message = "Nội dung chi tiết không được để trống")
    private String content; // Thêm trường này

    @NotBlank(message = "Địa điểm không được để trống")
    private String location;

    @Min(value = 0, message = "Giá tiền không được âm")
    private Double price;

    @Min(value = 1, message = "Số lượng người tham gia tối thiểu là 1")
    private Integer maxParticipants;

    private LocalDateTime startDate;
    private LocalDateTime endDate;
    private LocalDateTime registrationStartDate;
    private LocalDateTime registrationEndDate;

    // --- Thông tin bổ sung cho trang chi tiết ---
    private String targetAudience;
    private String tools;
    private String benefits;
}