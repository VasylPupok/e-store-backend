package com.shop.queue.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.GenericGenerator;

import java.time.LocalDateTime;

@Entity
@Table(name = "product_queues")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProductQueue {
    @Id
    @GeneratedValue(generator = "uuid")
    @GenericGenerator(name = "uuid", strategy = "uuid2")
    private String id;

    @Column(nullable = false, unique = true)
    private String productId;

    @Column(nullable = false)
    @Builder.Default
    private Integer queueLength = 0;

    @Column(nullable = false)
    @Builder.Default
    private boolean active = true;

    @Column
    private Integer availableStock;

    @Column(nullable = false)
    @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now();

    @Column(nullable = false)
    @Builder.Default
    private LocalDateTime updatedAt = LocalDateTime.now();

    @Version
    private Long version;

    @PreUpdate
    public void preUpdate() {
        this.updatedAt = LocalDateTime.now();
    }

    public void incrementQueueLength() {
        this.queueLength++;
    }

    public void decrementQueueLength() {
        if (this.queueLength > 0) {
            this.queueLength--;
        }
    }
}