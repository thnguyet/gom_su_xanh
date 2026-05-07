package org.gomsu.productservice.dto.request;

import jakarta.validation.constraints.NotBlank;
import jdk.jfr.Name;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CategoryUpdateRequest {
    private String name;
    private Boolean active;
    private Boolean deleteImage = false;
}
