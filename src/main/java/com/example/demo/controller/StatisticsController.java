package com.example.demo.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import com.example.demo.model.AccountStatus;
import com.example.demo.repository.UserRepository;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@RestController
@CrossOrigin(origins = { "http://localhost:3000", "http://localhost:8080" })
@RequestMapping("/api/statistics")
public class StatisticsController {

    private final UserRepository userRepository;

    public StatisticsController(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @GetMapping
    @PreAuthorize("hasAuthority('ADMIN')")
    public ResponseEntity<?> getStatistics() {
        long totalUsers = userRepository.count();
        long activeUsers = userRepository.findAll().stream()
                .filter(user -> user.getAccountStatus() == AccountStatus.ACTIVE)
                .count();
        long suspendedUsers = userRepository.findAll().stream()
                .filter(user -> user.getAccountStatus() == AccountStatus.SUSPENDED)
                .count();

        // Users created today
        LocalDateTime todayStart = LocalDateTime.now().toLocalDate().atStartOfDay();
        long usersToday = userRepository.findAll().stream()
                .filter(user -> user.getCreatedAt().isAfter(todayStart))
                .count();

        // Users created this week
        LocalDateTime weekStart = LocalDateTime.now().minusWeeks(1);
        long usersThisWeek = userRepository.findAll().stream()
                .filter(user -> user.getCreatedAt().isAfter(weekStart))
                .count();

        // Users created this month
        LocalDateTime monthStart = LocalDateTime.now().minusMonths(1);
        long usersThisMonth = userRepository.findAll().stream()
                .filter(user -> user.getCreatedAt().isAfter(monthStart))
                .count();

        // Email verified count
        long verifiedUsers = userRepository.findAll().stream()
                .filter(user -> user.isEmailVerified())
                .count();

        Map<String, Object> stats = new HashMap<>();
        stats.put("totalUsers", totalUsers);
        stats.put("activeUsers", activeUsers);
        stats.put("suspendedUsers", suspendedUsers);
        stats.put("usersToday", usersToday);
        stats.put("usersThisWeek", usersThisWeek);
        stats.put("usersThisMonth", usersThisMonth);
        stats.put("verifiedUsers", verifiedUsers);
        stats.put("unverifiedUsers", totalUsers - verifiedUsers);

        return ResponseEntity.ok(stats);
    }
}
