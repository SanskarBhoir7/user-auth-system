package com.example.demo.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import com.example.demo.repository.AuditLogRepository;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@CrossOrigin(origins = { "http://localhost:3000", "http://localhost:8080" })
@RequestMapping("/api/audit")
public class AuditController {

    private final AuditLogRepository auditLogRepository;

    public AuditController(AuditLogRepository auditLogRepository) {
        this.auditLogRepository = auditLogRepository;
    }

    @GetMapping("/logs")
    @PreAuthorize("hasAuthority('ADMIN')")
    public ResponseEntity<?> getAuditLogs(@RequestParam(defaultValue = "50") int limit) {
        List<Map<String, Object>> logs = auditLogRepository.findAllByOrderByTimestampDesc()
                .stream()
                .limit(limit)
                .map(log -> {
                    Map<String, Object> logMap = new HashMap<>();
                    logMap.put("id", log.getId());
                    logMap.put("action", log.getAction());
                    logMap.put("timestamp", log.getTimestamp().toString());
                    logMap.put("ipAddress", log.getIpAddress());
                    logMap.put("details", log.getDetails());
                    if (log.getUser() != null) {
                        logMap.put("username", log.getUser().getUsername());
                        logMap.put("userId", log.getUser().getId());
                    }
                    return logMap;
                })
                .collect(Collectors.toList());

        return ResponseEntity.ok(logs);
    }
}
