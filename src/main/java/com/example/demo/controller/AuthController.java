package com.example.demo.controller;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import com.example.demo.dto.LoginRequest;
import com.example.demo.dto.RegisterRequest;
import com.example.demo.dto.AuthResponse;
import com.example.demo.dto.ForgotPasswordRequest;
import com.example.demo.dto.ResetPasswordRequest;
import com.example.demo.model.AccountStatus;
import com.example.demo.model.EmailVerificationToken;
import com.example.demo.model.PasswordResetToken;
import com.example.demo.model.User;
import com.example.demo.repository.EmailVerificationTokenRepository;
import com.example.demo.repository.PasswordResetTokenRepository;
import com.example.demo.repository.UserRepository;
import com.example.demo.security.JwtUtil;
import com.example.demo.service.AuditService;
import com.example.demo.service.EmailService;
import com.example.demo.service.RateLimitService;

import java.time.LocalDateTime;
import java.util.UUID;

@RestController
@CrossOrigin(origins = { "http://localhost:3000", "http://localhost:8080" })
@RequestMapping("/api/auth")
public class AuthController {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;
    private final EmailService emailService;
    private final EmailVerificationTokenRepository verificationTokenRepository;
    private final PasswordResetTokenRepository passwordResetTokenRepository;
    private final RateLimitService rateLimitService;
    private final AuditService auditService;

    public AuthController(
            UserRepository userRepository,
            PasswordEncoder passwordEncoder,
            JwtUtil jwtUtil,
            EmailService emailService,
            EmailVerificationTokenRepository verificationTokenRepository,
            PasswordResetTokenRepository passwordResetTokenRepository,
            RateLimitService rateLimitService,
            AuditService auditService) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtUtil = jwtUtil;
        this.emailService = emailService;
        this.verificationTokenRepository = verificationTokenRepository;
        this.passwordResetTokenRepository = passwordResetTokenRepository;
        this.rateLimitService = rateLimitService;
        this.auditService = auditService;
    }

    @PostMapping("/register")
    public ResponseEntity<String> register(@RequestBody RegisterRequest request, HttpServletRequest httpRequest) {

        // Rate limiting
        String clientIp = getClientIp(httpRequest);
        if (!rateLimitService.tryConsume(clientIp + ":register")) {
            return ResponseEntity.status(429).body("Too many registration attempts. Please try again later.");
        }

        if (userRepository.findByUsername(request.getUsername()).isPresent()) {
            return ResponseEntity.badRequest().body("Username already exists");
        }

        if (userRepository.findByEmail(request.getEmail()).isPresent()) {
            return ResponseEntity.badRequest().body("Email already exists");
        }

        User user = new User(
                request.getUsername(),
                request.getEmail(),
                passwordEncoder.encode(request.getPassword()));

        userRepository.save(user);

        // Generate and send verification email
        try {
            String token = UUID.randomUUID().toString();
            EmailVerificationToken verificationToken = new EmailVerificationToken(
                    token, user, LocalDateTime.now().plusHours(24));
            verificationTokenRepository.save(verificationToken);

            emailService.sendVerificationEmail(user.getEmail(), user.getUsername(), token);
        } catch (Exception e) {
            // Log error but don't fail registration
            System.err.println("Failed to send verification email: " + e.getMessage());
        }

        // Log registration
        auditService.logAction(user, "USER_REGISTERED", clientIp, "New user registered");

        return ResponseEntity.ok("User registered successfully. Please check your email to verify your account.");
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody LoginRequest request, HttpServletRequest httpRequest) {

        // Rate limiting
        String clientIp = getClientIp(httpRequest);
        if (!rateLimitService.tryConsume(clientIp + ":login")) {
            auditService.logLoginAttempt(request.getUsername(), false, clientIp);
            return ResponseEntity.status(429).body("Too many login attempts. Please try again later.");
        }

        return userRepository.findByUsername(request.getUsername())
                .map(user -> {
                    if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
                        auditService.logLoginAttempt(request.getUsername(), false, clientIp);
                        return ResponseEntity.status(401).body("Invalid password");
                    }

                    // Check account status
                    if (user.getAccountStatus() == AccountStatus.SUSPENDED) {
                        auditService.logLoginAttempt(request.getUsername(), false, clientIp);
                        return ResponseEntity.status(403)
                                .body("Account suspended. Please contact administrator.");
                    }

                    if (user.getAccountStatus() == AccountStatus.DISABLED) {
                        auditService.logLoginAttempt(request.getUsername(), false, clientIp);
                        return ResponseEntity.status(403)
                                .body("Account disabled. Please contact administrator.");
                    }

                    // Update last login
                    user.setLastLogin(LocalDateTime.now());
                    userRepository.save(user);

                    // Log successful login
                    auditService.logLoginAttempt(request.getUsername(), true, clientIp);
                    auditService.logAction(user, "LOGIN_SUCCESS", clientIp, "User logged in");

                    String token = jwtUtil.generateToken(user.getUsername(), user.getRole().name());
                    return ResponseEntity.ok(new AuthResponse(token));
                })
                .orElseGet(() -> {
                    auditService.logLoginAttempt(request.getUsername(), false, clientIp);
                    return ResponseEntity.status(401).body("User not found");
                });
    }

    @GetMapping("/verify-email")
    public ResponseEntity<String> verifyEmail(@RequestParam String token) {
        return verificationTokenRepository.findByToken(token)
                .map(verificationToken -> {
                    if (verificationToken.isExpired()) {
                        return ResponseEntity.badRequest().body("Verification link has expired");
                    }

                    if (verificationToken.isVerified()) {
                        return ResponseEntity.ok("Email already verified");
                    }

                    User user = verificationToken.getUser();
                    user.setEmailVerified(true);
                    userRepository.save(user);

                    verificationToken.setVerified(true);
                    verificationTokenRepository.save(verificationToken);

                    auditService.logAction(user, "EMAIL_VERIFIED", "N/A", "User verified email");

                    return ResponseEntity.ok("Email verified successfully! You can now login.");
                })
                .orElse(ResponseEntity.badRequest().body("Invalid verification token"));
    }

    @PostMapping("/forgot-password")
    public ResponseEntity<String> forgotPassword(
            @RequestBody ForgotPasswordRequest request,
            HttpServletRequest httpRequest) {

        String clientIp = getClientIp(httpRequest);

        return userRepository.findByEmail(request.getEmail())
                .map(user -> {
                    try {
                        // Delete any existing tokens for this user
                        passwordResetTokenRepository.deleteByUserId(user.getId());

                        // Create new token
                        String token = UUID.randomUUID().toString();
                        PasswordResetToken resetToken = new PasswordResetToken(
                                token, user, LocalDateTime.now().plusHours(1));
                        passwordResetTokenRepository.save(resetToken);

                        // Send reset email
                        emailService.sendPasswordResetEmail(user.getEmail(), user.getUsername(), token);

                        auditService.logAction(user, "PASSWORD_RESET_REQUESTED", clientIp,
                                "Password reset email sent");

                        return ResponseEntity.ok("Password reset link has been sent to your email");
                    } catch (Exception e) {
                        e.printStackTrace();
                        return ResponseEntity.status(500).body("Failed to send reset email: " + e.getMessage());
                    }
                })
                .orElse(ResponseEntity.ok("If that email exists, a reset link has been sent"));
    }

    @PostMapping("/reset-password")
    public ResponseEntity<String> resetPassword(
            @RequestBody ResetPasswordRequest request,
            HttpServletRequest httpRequest) {

        String clientIp = getClientIp(httpRequest);

        return passwordResetTokenRepository.findByToken(request.getToken())
                .map(resetToken -> {
                    if (resetToken.isExpired()) {
                        return ResponseEntity.badRequest().body("Reset link has expired");
                    }

                    if (resetToken.isUsed()) {
                        return ResponseEntity.badRequest().body("Reset link has already been used");
                    }

                    User user = resetToken.getUser();
                    user.setPassword(passwordEncoder.encode(request.getNewPassword()));
                    userRepository.save(user);

                    resetToken.setUsed(true);
                    passwordResetTokenRepository.save(resetToken);

                    auditService.logAction(user, "PASSWORD_RESET_COMPLETED", clientIp,
                            "User reset password successfully");

                    return ResponseEntity.ok("Password reset successfully");
                })
                .orElse(ResponseEntity.badRequest().body("Invalid reset token"));
    }

    private String getClientIp(HttpServletRequest request) {
        String xForwardedFor = request.getHeader("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.isEmpty()) {
            return xForwardedFor.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }
}
