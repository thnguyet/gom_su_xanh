package org.gomsu.orderservice.dto;

import lombok.Data;

import java.util.List;

@Data
public class ProductDTO {
    private Long id;
    private String name;
    private Double price;
    private List<String> imageUrls;
    private Integer stockQuantity;
}
