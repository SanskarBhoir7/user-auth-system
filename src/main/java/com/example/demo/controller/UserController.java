package com.example.demo.controller;

import java.util.List;
import java.util.stream.Collectors;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import com.example.demo.dto.UserDTO;
import com.example.demo.model.AccountStatus;
import com.example.demo.model.Role;
import com.example.demo.model.User;
import com.example.demo.repository.AuditLogRepository;
import com.example.demo.repository.EmailVerificationTokenRepository;
import com.example.demo.repository.PasswordResetTokenRepository;
import com.example.demo.repository.UserRepository;

import jakarta.persistence.criteria.Predicate;
import java.util.ArrayList;

@RestController
@CrossOrigin(origins = { "http://localhost:3000", "http://localhost:8080" })
@RequestMapping("/api/users")
public class UserController {

    private final UserRepository userRepository;
    private final EmailVerificationTokenRepository emailVerificationTokenRepository;
    private final PasswordResetTokenRepository passwordResetTokenRepository;
    private final AuditLogRepository auditLogRepository;

    public UserController(
            UserRepository userRepository,
            EmailVerificationTokenRepository emailVerificationTokenRepository,
            PasswordResetTokenRepository passwordResetTokenRepository,
            AuditLogRepository auditLogRepository) {
        this.userRepository = userRepository;
        this.emailVerificationTokenRepository = emailVerificationTokenRepository;
        this.passwordResetTokenRepository = passwordResetTokenRepository;
        this.auditLogRepository = auditLogRepository;
    }

    @GetMapping
    public ResponseEntity<?> getAllUsers(
            @RequestParam(required = false) String search,
            @RequestParam(required = false) String role,
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "100") int size) {

        Pageable pageable = PageRequest.of(page, size);

        // Build dynamic specification for filtering
        Specification<User> spec = (root, query, criteriaBuilder) -> {
            List<Predicate> predicates = new ArrayList<>();

            // Search by username or email
            if (search != null && !search.trim().isEmpty()) {
                String searchPattern = "%" + search.toLowerCase() + "%";
                Predicate usernamePredicate = criteriaBuilder.like(
                        criteriaBuilder.lower(root.get("username")), searchPattern);
                Predicate emailPredicate = criteriaBuilder.like(
                        criteriaBuilder.lower(root.get("email")), searchPattern);
                predicates.add(criteriaBuilder.or(usernamePredicate, emailPredicate));
            }

            // Filter by role
            if (role != null && !role.trim().isEmpty()) {
                try {
                    predicates.add(criteriaBuilder.equal(root.get("role"), Role.valueOf(role.toUpperCase())));
                } catch (IllegalArgumentException e) {
                    // Invalid role, ignore
                }
            }

            // Filter by status
            if (status != null && !status.trim().isEmpty()) {
                try {
                    predicates.add(criteriaBuilder.equal(root.get("accountStatus"),
                            AccountStatus.valueOf(status.toUpperCase())));
                } catch (IllegalArgumentException e) {
                    // Invalid status, ignore
                }
            }

            return criteriaBuilder.and(predicates.toArray(new Predicate[0]));
        };

        Page<User> usersPage = userRepository.findAll(spec, pageable);

        List<UserDTO> users = usersPage.getContent()
                .stream()
                .map(user -> new UserDTO(
                        user.getId(),
                        user.getUsername(),
                        user.getEmail(),
                        user.getRole().name(),
                        user.getAccountStatus().name()))
                .collect(Collectors.toList());

        // Return paginated response
        return ResponseEntity.ok(new java.util.HashMap<String, Object>() {
            {
                put("users", users);
                put("currentPage", usersPage.getNumber());
                put("totalItems", usersPage.getTotalElements());
                put("totalPages", usersPage.getTotalPages());
            }
        });
    }

    @PutMapping("/{id}/status")
    @PreAuthorize("hasAuthority('ADMIN')")
    public ResponseEntity<String> updateUserStatus(
            @PathVariable Long id,
            @RequestBody java.util.Map<String, String> request) {

        return userRepository.findById(id)
                .map(user -> {
                    try {
                        AccountStatus newStatus = AccountStatus.valueOf(request.get("status").toUpperCase());
                        user.setAccountStatus(newStatus);
                        userRepository.save(user);
                        return ResponseEntity.ok("User status updated successfully");
                    } catch (IllegalArgumentException e) {
                        return ResponseEntity.badRequest().body("Invalid status value");
                    }
                })
                .orElse(ResponseEntity.notFound().build());
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('ADMIN')")
    public ResponseEntity<String> deleteUser(@PathVariable Long id) {
        return userRepository.findById(id)
                .map(user -> {
                    try {
                        // Delete related records first to avoid foreign key constraints
                        emailVerificationTokenRepository.deleteByUserId(id);
                        passwordResetTokenRepository.deleteByUserId(id);
                        auditLogRepository.deleteByUserId(id);

                        // Now delete the user
                        userRepository.delete(user);

                        return ResponseEntity.ok("User deleted successfully");
                    } catch (Exception e) {
                        return ResponseEntity.status(500)
                                .body("Failed to delete user: " + e.getMessage());
                    }
                })
                .orElse(ResponseEntity.notFound().build());
    }
}
