package com.example.demo.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import com.example.demo.dto.UserDTO;
import com.example.demo.model.Theme;
import com.example.demo.model.User;
import com.example.demo.repository.UserRepository;

import java.util.Map;

@RestController
@CrossOrigin(origins = { "http://localhost:3000", "http://localhost:8080" })
@RequestMapping("/api/profile")
public class UserProfileController {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public UserProfileController(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @GetMapping
    public ResponseEntity<?> getCurrentUserProfile(Authentication authentication) {
        String username = authentication.getName();

        return userRepository.findByUsername(username)
                .map(user -> ResponseEntity.ok(Map.of(
                        "id", user.getId(),
                        "username", user.getUsername(),
                        "email", user.getEmail(),
                        "role", user.getRole().name(),
                        "accountStatus", user.getAccountStatus().name(),
                        "emailVerified", user.isEmailVerified(),
                        "theme", user.getTheme().name(),
                        "createdAt", user.getCreatedAt().toString(),
                        "lastLogin", user.getLastLogin() != null ? user.getLastLogin().toString() : null)))
                .orElse(ResponseEntity.notFound().build());
    }

    @PutMapping
    public ResponseEntity<String> updateProfile(
            Authentication authentication,
            @RequestBody Map<String, String> updates) {

        String username = authentication.getName();

        return userRepository.findByUsername(username)
                .map(user -> {
                    // Update username if provided
                    if (updates.containsKey("username") && !updates.get("username").trim().isEmpty()) {
                        String newUsername = updates.get("username");
                        if (!newUsername.equals(user.getUsername()) &&
                                userRepository.findByUsername(newUsername).isPresent()) {
                            return ResponseEntity.badRequest().body("Username already exists");
                        }
                        user.setUsername(newUsername);
                    }

                    // Update email if provided
                    if (updates.containsKey("email") && !updates.get("email").trim().isEmpty()) {
                        String newEmail = updates.get("email");
                        if (!newEmail.equals(user.getEmail()) &&
                                userRepository.findByEmail(newEmail).isPresent()) {
                            return ResponseEntity.badRequest().body("Email already exists");
                        }
                        user.setEmail(newEmail);
                        user.setEmailVerified(false); // Re-verify if email changed
                    }

                    userRepository.save(user);
                    return ResponseEntity.ok("Profile updated successfully");
                })
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping("/change-password")
    public ResponseEntity<String> changePassword(
            Authentication authentication,
            @RequestBody Map<String, String> request) {

        String username = authentication.getName();
        String currentPassword = request.get("currentPassword");
        String newPassword = request.get("newPassword");

        if (currentPassword == null || newPassword == null) {
            return ResponseEntity.badRequest().body("Current and new passwords are required");
        }

        if (newPassword.length() < 6) {
            return ResponseEntity.badRequest().body("New password must be at least 6 characters");
        }

        return userRepository.findByUsername(username)
                .map(user -> {
                    if (!passwordEncoder.matches(currentPassword, user.getPassword())) {
                        return ResponseEntity.badRequest().body("Current password is incorrect");
                    }

                    user.setPassword(passwordEncoder.encode(newPassword));
                    userRepository.save(user);

                    return ResponseEntity.ok("Password changed successfully");
                })
                .orElse(ResponseEntity.notFound().build());
    }

    @PutMapping("/theme")
    public ResponseEntity<String> updateTheme(
            Authentication authentication,
            @RequestBody Map<String, String> request) {

        String username = authentication.getName();
        String themeStr = request.get("theme");

        return userRepository.findByUsername(username)
                .map(user -> {
                    try {
                        Theme theme = Theme.valueOf(themeStr.toUpperCase());
                        user.setTheme(theme);
                        userRepository.save(user);
                        return ResponseEntity.ok("Theme updated successfully");
                    } catch (IllegalArgumentException e) {
                        return ResponseEntity.badRequest().body("Invalid theme value");
                    }
                })
                .orElse(ResponseEntity.notFound().build());
    }
}
