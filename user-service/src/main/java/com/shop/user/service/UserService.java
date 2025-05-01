package com.shop.user.service;

import com.shop.common.exception.ResourceNotFoundException;
import com.shop.user.dto.UserDto;
import com.shop.user.dto.UserProfileDto;
import com.shop.user.entity.User;
import com.shop.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;

    @Transactional(readOnly = true)
    public UserDto getUserById(String id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + id));

        return mapToUserDto(user);
    }

    @Transactional(readOnly = true)
    public UserProfileDto getUserProfile(String id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + id));

        return mapToUserProfileDto(user);
    }

    @Transactional
    public UserProfileDto updateUserProfile(String id, UserProfileDto profileDto) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + id));

        // Update user fields
        user.setName(profileDto.getName());
        user.setAddress(profileDto.getAddress());
        user.setPhone(profileDto.getPhone());

        user = userRepository.save(user);

        return mapToUserProfileDto(user);
    }

    private UserDto mapToUserDto(User user) {
        return UserDto.builder()
                .id(user.getId())
                .email(user.getEmail())
                .name(user.getName())
                .createdAt(user.getCreatedAt())
                .build();
    }

    private UserProfileDto mapToUserProfileDto(User user) {
        return UserProfileDto.builder()
                .id(user.getId())
                .email(user.getEmail())
                .name(user.getName())
                .address(user.getAddress())
                .phone(user.getPhone())
                .build();
    }
}