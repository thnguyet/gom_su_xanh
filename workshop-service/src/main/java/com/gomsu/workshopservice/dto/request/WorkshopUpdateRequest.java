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
    private LocalDateTime startDate;
    private LocalDateTime endDate;

    // Thời gian đăng ký
    private LocalDateTime registrationStartDate;
    private LocalDateTime registrationEndDate;

    // Thông tin bổ sung
    private String targetAudience;
    private String tools;
    private String benefits;

    // Danh sách ID các ảnh cũ mà Admin muốn xóa
    private List<Long> deletedImageIds;
}