package com.shop.cart.entity;

import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.Version;
import org.springframework.data.mongodb.core.mapping.Document;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Document(collection = "carts")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Cart {
    @Id
    private String id;

    private String userId;

    @Builder.Default
    private List<CartItem> items = new ArrayList<>();

    private BigDecimal totalPrice;

    @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now();

    @Builder.Default
    private LocalDateTime updatedAt = LocalDateTime.now();

    @Version
    private Long version;

    public void calculateTotalPrice() {
        this.totalPrice = items.stream()
                .map(item -> item.getPrice().multiply(BigDecimal.valueOf(item.getQuantity())))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    public void addItem(CartItem item) {
        // ѕерев≥р€Їмо, чи Ї вже такий товар у кошику
        for (CartItem existingItem : items) {
            if (existingItem.getProductId().equals(item.getProductId())) {
                // якщо товар обмежений, не додаЇмо його, а створюЇмо новий
                if (item.isLimitedProduct()) {
                    items.add(item);
                    calculateTotalPrice();
                    return;
                }

                // ƒл€ звичайних товар≥в зб≥льшуЇмо к≥льк≥сть
                existingItem.setQuantity(existingItem.getQuantity() + item.getQuantity());
                calculateTotalPrice();
                return;
            }
        }

        // якщо такого товару ще немаЇ, додаЇмо його
        items.add(item);
        calculateTotalPrice();
    }

    public void removeItem(String itemId) {
        items.removeIf(item -> item.getId().equals(itemId));
        calculateTotalPrice();
    }

    public void updateTimestamps() {
        this.updatedAt = LocalDateTime.now();
    }
}