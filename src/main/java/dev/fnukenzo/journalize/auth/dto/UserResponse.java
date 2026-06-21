package dev.fnukenzo.journalize.auth.dto;

import java.time.LocalDateTime;

import dev.fnukenzo.journalize.user.User;

public record UserResponse(Long id, String username, String email, LocalDateTime createdAt) {

    public static UserResponse from(User user) {
        return new UserResponse(user.getId(), user.getUsername(), user.getEmail(), user.getCreatedAt());
    }

}
