package com.era.backend.user.controller;

import com.era.backend.common.ApiResponse;
import com.era.backend.user.model.UpdateProfileRequest;
import com.era.backend.user.model.User;
import com.era.backend.user.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    @GetMapping("/me")
    public ResponseEntity<ApiResponse<User>> getMe() {
        User user = userService.getById(currentUserId());
        return ResponseEntity.ok(ApiResponse.ok(user, "Current user"));
    }

    @PutMapping("/me")
    public ResponseEntity<ApiResponse<User>> updateMe(@RequestBody UpdateProfileRequest request) {
        User user = userService.updateProfile(currentUserId(), request);
        return ResponseEntity.ok(ApiResponse.ok(user, "Profile updated"));
    }

    @GetMapping("/search")
    public ResponseEntity<ApiResponse<List<User>>> search(@RequestParam String query) {
        List<User> users = userService.searchByUsername(query);
        return ResponseEntity.ok(ApiResponse.ok(users, "Search results"));
    }

    private String currentUserId() {
        return SecurityContextHolder.getContext().getAuthentication().getName();
    }
}
