package com.shop.common.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProductDto {
    private String id;
    private String name;
    private BigDecimal price;
    private String category;
    private String image;
    private boolean isLimited;
    private Integer stockQuantity;
    private Integer reservedQuantity;
}
