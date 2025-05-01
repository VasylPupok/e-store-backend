package com.shop.cart.controller;

import com.shop.cart.dto.AddToCartRequest;
import com.shop.cart.dto.AddToCartResponse;
import com.shop.cart.service.CartService;
import com.shop.common.dto.CartDto;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/carts")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Cart", description = "Операції з кошиком користувача")
public class CartController {
    private final CartService cartService;

    @GetMapping
    @Operation(summary = "Отримати кошик поточного користувача", description = "Повертає вміст кошика поточного авторизованого користувача")
    public ResponseEntity<CartDto> getCart(@RequestHeader("X-User-Id") String userId) {
        CartDto cart = cartService.getCartByUserId(userId);
        return ResponseEntity.ok(cart);
    }

    @PostMapping("/items")
    @Operation(summary = "Додати товар у кошик", description = "Додає товар у кошик або запускає процес постановки в чергу, якщо товар має обмежений залишок")
    public ResponseEntity<AddToCartResponse> addItemToCart(
            @RequestHeader("X-User-Id") String userId,
            @Valid @RequestBody AddToCartRequest request) {

        AddToCartResponse response = cartService.addItemToCart(userId, request);
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/items/{itemId}")
    @Operation(summary = "Видалити товар з кошика", description = "Видаляє товар з кошика та скасовує його резервацію")
    public ResponseEntity<Void> removeItemFromCart(
            @RequestHeader("X-User-Id") String userId,
            @PathVariable String itemId) {

        cartService.removeItemFromCart(userId, itemId);
        return ResponseEntity.ok().build();
    }
}
