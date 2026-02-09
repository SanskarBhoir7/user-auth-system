package com.example.demo.service;

import com.example.demo.model.AuditLog;
import com.example.demo.model.User;
import com.example.demo.repository.AuditLogRepository;
import org.springframework.stereotype.Service;

@Service
public class AuditService {

    private final AuditLogRepository auditLogRepository;

    public AuditService(AuditLogRepository auditLogRepository) {
        this.auditLogRepository = auditLogRepository;
    }

    public void logAction(User user, String action, String ipAddress, String details) {
        AuditLog log = new AuditLog(user, action, ipAddress, details);
        auditLogRepository.save(log);
    }

    public void logLoginAttempt(String username, boolean successful, String ipAddress) {
        String action = successful ? "LOGIN_SUCCESS" : "LOGIN_FAILED";
        String details = "User: " + username;
        AuditLog log = new AuditLog(null, action, ipAddress, details);
        auditLogRepository.save(log);
    }
}
