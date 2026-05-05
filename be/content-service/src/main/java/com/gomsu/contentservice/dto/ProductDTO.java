package com.gomsu.contentservice.dto;

import lombok.Data;

@Data
public class ProductDTO {
    private Long id;
    private String name;
    private Double price;
    private String imageUrl;
    private Integer stockQuantity;
}
