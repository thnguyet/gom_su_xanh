package org.gomsu.productservice.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CategoryResponse {
    private Long id;
    private String name;
    private String slug; // Thêm slug
    private Integer productCount;
    private String imageUrl;
    private boolean active;
    private LocalDateTime createdAt;
}
