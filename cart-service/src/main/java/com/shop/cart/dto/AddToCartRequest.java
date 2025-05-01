package com.shop.cart.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AddToCartRequest {
    @NotBlank(message = "Product ID must not be blank")
    private String productId;

    @Min(value = 1, message = "Quantity must be at least 1")
    private Integer quantity;
}