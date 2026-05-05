package org.gomsu.productservice.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class CategoryCreationRequest {
    @NotBlank(message = "Không được để trống tên bộ sưu tập!")
    private String name;
}
