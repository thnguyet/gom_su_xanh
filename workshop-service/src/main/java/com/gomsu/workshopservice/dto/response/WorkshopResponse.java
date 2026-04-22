package com.gomsu.workshopservice.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WorkshopResponse {
    private Long id;
    private String name;
    private String description;
    private String content; // Trả về nội dung chi tiết
    private String location;
    private Double price;
    private Integer maxParticipants;
    private Integer currentParticipants;

    private LocalDateTime startDate;
    private LocalDateTime endDate;
    private LocalDateTime registrationStartDate;
    private LocalDateTime registrationEndDate;

    // --- Các trường thông tin bổ sung ---
    private String targetAudience;
    private String tools;
    private String benefits;

    private String mainImage;
    private List<String> allImages;
}