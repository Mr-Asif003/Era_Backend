package com.era.backend.user.service;

import com.era.backend.user.model.UpdateProfileRequest;
import com.era.backend.user.model.User;
import com.era.backend.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;
import java.util.NoSuchElementException;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;

    public User getById(String userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new NoSuchElementException("User not found"));
    }

    public User updateProfile(String userId, UpdateProfileRequest req) {
        User user = getById(userId);

        if (req.getDisplayName() != null) user.setDisplayName(req.getDisplayName());
        if (req.getBio() != null) user.setBio(req.getBio());
        if (req.getAvatarUrl() != null) user.setAvatarUrl(req.getAvatarUrl());
        if (req.getAvatarColor() != null) user.setAvatarColor(req.getAvatarColor());
        if (req.getEraVoice() != null) user.setEraVoice(req.getEraVoice());
        if (req.getEraLanguage() != null) user.setEraLanguage(req.getEraLanguage());

        user.setUpdatedAt(Instant.now());
        return userRepository.save(user);
    }

    public List<User> searchByUsername(String query) {
        return userRepository.findByUsernameContainingIgnoreCase(query);
    }
}
