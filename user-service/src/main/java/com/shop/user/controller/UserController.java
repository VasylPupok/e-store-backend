package com.shop.user.controller;

import com.shop.user.dto.*;
import com.shop.user.service.AuthService;
import com.shop.user.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/users")
@RequiredArgsConstructor
@Tag(name = "User", description = "Операції з користувачами")
public class UserController {

    private final AuthService authService;
    private final UserService userService;

    @PostMapping("/register")
    @Operation(summary = "Реєстрація нового користувача", description = "Створює новий обліковий запис користувача")
    public ResponseEntity<AuthResponse> register(@Valid @RequestBody RegisterRequest request) {
        AuthResponse response = authService.register(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PostMapping("/login")
    @Operation(summary = "Автентифікація користувача", description = "Логін користувача в систему")
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody LoginRequest request) {
        AuthResponse response = authService.login(request);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/me")
    @Operation(summary = "Отримати інформацію про поточного користувача", description = "Повертає дані профілю авторизованого користувача")
    public ResponseEntity<UserProfileDto> getCurrentUser(@RequestHeader("X-User-Id") String userId) {
        UserProfileDto profile = userService.getUserProfile(userId);
        return ResponseEntity.ok(profile);
    }

    @PutMapping("/me")
    @Operation(summary = "Оновити профіль користувача", description = "Оновлює дані профілю авторизованого користувача")
    public ResponseEntity<UserProfileDto> updateUserProfile(
            @RequestHeader("X-User-Id") String userId,
            @Valid @RequestBody UserProfileDto profileDto) {
        UserProfileDto updatedProfile = userService.updateUserProfile(userId, profileDto);
        return ResponseEntity.ok(updatedProfile);
    }
}