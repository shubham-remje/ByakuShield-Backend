package com.byakushield.backend.service;

import com.byakushield.backend.dto.OrchestratorResponse;
import com.byakushield.backend.dto.ThreatResponse;
import com.byakushield.backend.model.Audit;
import com.byakushield.backend.model.User;
import com.byakushield.backend.repository.AuditRepository;
import com.byakushield.backend.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class AuditService {

    private final AuditRepository auditRepository;
    private final UserRepository userRepository;

    public AuditService(
            AuditRepository auditRepository,
            UserRepository userRepository
    ) {
        this.auditRepository = auditRepository;
        this.userRepository = userRepository;
    }

    @Transactional
    public Audit saveThreatAudit(
            String username,
            ThreatResponse response
    ) {

        User user = userRepository.findByUsername(username)
                .orElseThrow(() ->
                        new RuntimeException(
                                "User not found: " + username
                        )
                );

        Map<String, Object> resultData = new HashMap<>();

        resultData.put("module", response.getModule());
        resultData.put("threatLevel", response.getThreatLevel());
        resultData.put("riskScore", response.getRiskScore());
        resultData.put("details", response.getDetails());

        Audit audit = new Audit(
                user,
                response.getModule(),
                response.getThreatLevel(),
                response.getRiskScore(),
                response.getDetails(),
                resultData
        );

        return auditRepository.save(audit);
    }

    @Transactional
    public Audit saveOrchestratorAudit(
            String username,
            OrchestratorResponse response
    ) {

        User user = userRepository.findByUsername(username)
                .orElseThrow(() ->
                        new RuntimeException(
                                "User not found: " + username
                        )
                );

        List<Map<String, Object>> moduleResults =
                response.getModuleResults()
                        .stream()
                        .map(result -> {
                            Map<String, Object> module =
                                    new HashMap<>();

                            module.put(
                                    "module",
                                    result.getModule()
                            );

                            module.put(
                                    "threatLevel",
                                    result.getThreatLevel()
                            );

                            module.put(
                                    "riskScore",
                                    result.getRiskScore()
                            );

                            module.put(
                                    "details",
                                    result.getDetails()
                            );

                            return module;
                        })
                        .toList();

        Map<String, Object> resultData =
                new HashMap<>();

        resultData.put(
                "verdict",
                response.getVerdict()
        );

        resultData.put(
                "riskScore",
                response.getRiskScore()
        );

        resultData.put(
                "moduleResults",
                moduleResults
        );

        Audit audit = new Audit(
                user,
                "Orchestrator",
                response.getVerdict(),
                response.getRiskScore(),
                "Multimodal security analysis completed.",
                resultData
        );

        return auditRepository.save(audit);
    }
}