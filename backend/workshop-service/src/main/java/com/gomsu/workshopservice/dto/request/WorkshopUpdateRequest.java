package com.gomsu.workshopservice.dto.request;

import lombok.*;
import java.time.LocalDateTime;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class WorkshopUpdateRequest {
    private String name;
    private String description;
    private String content;
    private String location;
    private Double price;
    private Integer maxParticipants;

    // Thời gian tổ chức
    @com.fasterxml.jackson.annotation.JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm")
    private LocalDateTime startDate;
    @com.fasterxml.jackson.annotation.JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm")
    private LocalDateTime endDate;

    // Thời gian đăng ký
    @com.fasterxml.jackson.annotation.JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm")
    private LocalDateTime registrationStartDate;
    @com.fasterxml.jackson.annotation.JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm")
    private LocalDateTime registrationEndDate;

    // Thông tin bổ sung
    private String targetAudience;
    private String tools;
    private String benefits;

    // Danh sách ID các ảnh cũ mà Admin muốn xóa
    private List<Long> deletedImageIds;

    private Boolean active;
}