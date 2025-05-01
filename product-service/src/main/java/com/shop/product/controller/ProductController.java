package com.shop.product.controller;

import com.shop.common.dto.ProductDto;
import com.shop.product.dto.ProductAvailabilityDto;
import com.shop.product.service.ProductService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/products")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Products", description = "Операції з товарами")
public class ProductController {
    private final ProductService productService;

    @GetMapping
    @Operation(summary = "Отримати список товарів", description = "Повертає список товарів з можливістю фільтрації та пагінації")
    public ResponseEntity<Page<ProductDto>> getAllProducts(
            @RequestParam(required = false) String category,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {

        Page<ProductDto> products = productService.findAll(category, page, size);
        return ResponseEntity.ok(products);
    }

    @GetMapping("/{productId}")
    @Operation(summary = "Отримати деталі товару", description = "Повертає детальну інформацію про товар, включаючи залишок на складі")
    public ResponseEntity<ProductDto> getProductById(@PathVariable String productId) {
        ProductDto product = productService.findById(productId);
        return ResponseEntity.ok(product);
    }

    @GetMapping("/{productId}/availability")
    @Operation(summary = "Отримати інформацію про доступність товару", description = "Повертає поточний залишок товару та інформацію про його доступність для покупки")
    public ResponseEntity<ProductAvailabilityDto> getProductAvailability(@PathVariable String productId) {
        ProductAvailabilityDto availability = productService.checkAvailability(productId);
        return ResponseEntity.ok(availability);
    }

    @PostMapping
    @Operation(summary = "Створити новий товар", description = "Створює новий товар в системі")
    public ResponseEntity<ProductDto> createProduct(@Valid @RequestBody ProductDto productDto) {
        ProductDto createdProduct = productService.create(productDto);
        return ResponseEntity.status(HttpStatus.CREATED).body(createdProduct);
    }

    @PutMapping("/{productId}")
    @Operation(summary = "Оновити інформацію про товар", description = "Оновлює інформацію про товар")
    public ResponseEntity<ProductDto> updateProduct(
            @PathVariable String productId,
            @Valid @RequestBody ProductDto productDto) {

        ProductDto updatedProduct = productService.update(productId, productDto);
        return ResponseEntity.ok(updatedProduct);
    }

    // Внутрішній API для резервації товару (викликається з інших сервісів)
    @PostMapping("/{productId}/reserve")
    public ResponseEntity<Void> reserveProduct(
            @PathVariable String productId,
            @RequestParam Integer quantity,
            @RequestParam String userId) {

        productService.reserveProduct(productId, quantity, userId);
        return ResponseEntity.ok().build();
    }

    // Внутрішній API для звільнення резервації (викликається з інших сервісів)
    @PostMapping("/{productId}/release")
    public ResponseEntity<Void> releaseReservation(
            @PathVariable String productId,
            @RequestParam Integer quantity) {

        productService.releaseReservation(productId, quantity);
        return ResponseEntity.ok().build();
    }
}