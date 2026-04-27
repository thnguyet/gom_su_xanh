package com.gomsu.contentservice.dto.response;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReviewResponse {
    private Long id;
    private Long productId;
    private String productName;

    @JsonIgnore
    private String username;

    private Integer rating;
    private String comment;
    private String imageReview;
    private boolean isVerifiedPurchase;
    private LocalDateTime createdAt;

    // --- BỔ SUNG NGHIỆP VỤ PHẢN HỒI ---
    private String adminReply;      // Nội dung Admin trả lời khách
    private LocalDateTime repliedAt; // Thời gian phản hồi

    // --- BỔ SUNG TRẠNG THÁI (Cho trang Admin) ---
    private boolean isApproved;

    @JsonProperty("username")
    public String getMaskedUsername() {
        if (username == null || username.isBlank()) return "Khách hàng gốm sứ";
        int len = username.length();
        if (len < 2) return "*";
        return username.charAt(0) + "*".repeat(len - 2) + username.charAt(len - 1);
    }

    // --- NGHIỆP VỤ 3: TỰ ĐỘNG GẮN NHÃN (FE dùng để hiện badge màu sắc) ---
    @JsonProperty("label")
    public String getReviewLabel() {
        if (rating == null) return "";
        return switch (rating) {
            case 5 -> "Cực kỳ hài lòng";
            case 4 -> "Hài lòng";
            case 3 -> "Bình thường";
            case 2 -> "Không hài lòng";
            case 1 -> "Tệ";
            default -> "";
        };
    }
}