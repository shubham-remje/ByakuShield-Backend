package com.byakushield.backend.controller;

import com.byakushield.backend.dto.*;
import com.byakushield.backend.model.Audit;
import com.byakushield.backend.model.User;
import com.byakushield.backend.repository.AuditRepository;
import com.byakushield.backend.repository.UserRepository;
import com.byakushield.backend.service.*;

import jakarta.validation.Valid;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/shield")
public class ShieldController {

    private final TextArmorService textArmorService;
    private final QuishGuardService quishGuardService;
    private final VoiceShieldService voiceShieldService;
    private final DataScrubService dataScrubService;

    private final OrchestratorService orchestratorService;
    private final AuditService auditService;

    private final AuditRepository auditRepository;
    private final UserRepository userRepository;

    public ShieldController(
            TextArmorService textArmorService,
            QuishGuardService quishGuardService,
            VoiceShieldService voiceShieldService,
            DataScrubService dataScrubService,
            OrchestratorService orchestratorService,
            AuditService auditService,
            AuditRepository auditRepository,
            UserRepository userRepository
    ) {
        this.textArmorService = textArmorService;
        this.quishGuardService = quishGuardService;
        this.voiceShieldService = voiceShieldService;
        this.dataScrubService = dataScrubService;
        this.orchestratorService = orchestratorService;
        this.auditService = auditService;
        this.auditRepository = auditRepository;
        this.userRepository = userRepository;
    }

    @PostMapping("/text-armor")
    public ResponseEntity<ThreatResponse> scanText(
            @Valid @RequestBody TextArmorRequest request,
            Authentication auth
    ) {

        ThreatResponse response =
                textArmorService.analyze(request);

        auditService.saveThreatAudit(
                auth.getName(),
                response
        );

        return ResponseEntity.ok(response);
    }

    @PostMapping("/quish-guard")
    public ResponseEntity<ThreatResponse> scanUrl(
            @RequestBody QuishGuardRequest request,
            Authentication auth
    ) {

        ThreatResponse response =
                quishGuardService.analyze(request);

        auditService.saveThreatAudit(
                auth.getName(),
                response
        );

        return ResponseEntity.ok(response);
    }

    @PostMapping("/quish-guard/qr")
    public ResponseEntity<ThreatResponse> scanQr(
            @RequestParam("file") MultipartFile file,
            Authentication auth
    ) {

        ThreatResponse response =
                quishGuardService.analyzeQr(file);

        auditService.saveThreatAudit(
                auth.getName(),
                response
        );

        return ResponseEntity.ok(response);
    }

    @PostMapping("/voice-shield")
    public ResponseEntity<ThreatResponse> scanAudio(
            @RequestParam("file") MultipartFile file,
            Authentication auth
    ) {

        ThreatResponse response =
                voiceShieldService.analyze(file);

        auditService.saveThreatAudit(
                auth.getName(),
                response
        );

        return ResponseEntity.ok(response);
    }

    @PostMapping("/data-scrub")
    public ResponseEntity<ThreatResponse> scrubMedia(
            @RequestParam("file") MultipartFile file,
            Authentication auth
    ) {

        /*
         * DataScrub returns ThreatResponse directly.
         * The response may contain sanitizedData,
         * sanitizedFilename and privacyReminder.
         */
        ThreatResponse response =
                dataScrubService.scrub(file);

        auditService.saveThreatAudit(
                auth.getName(),
                response
        );

        return ResponseEntity.ok(response);
    }

    @PostMapping(
            value = "/orchestrate",
            consumes = "multipart/form-data"
    )
    public ResponseEntity<OrchestratorResponse> orchestrate(
            @RequestParam(
                    value = "text",
                    required = false
            )
            String text,

            @RequestParam(
                    value = "url",
                    required = false
            )
            String url,

            @RequestParam(
                    value = "audio",
                    required = false
            )
            MultipartFile audio,

            @RequestParam(
                    value = "media",
                    required = false
            )
            MultipartFile media,

            @RequestParam(
                    value = "qr",
                    required = false
            )
            MultipartFile qr,

            Authentication authentication
    ) {

        OrchestratorRequest request =
                new OrchestratorRequest();

        request.setText(text);
        request.setUrl(url);

        OrchestratorResponse response =
                orchestratorService.analyze(
                        request,
                        audio,
                        media,
                        qr,
                        authentication.getName()
                );

        return ResponseEntity.ok(response);
    }

    @GetMapping("/audit-history")
    public ResponseEntity<Page<AuditHistoryResponse>>
    getAuditHistory(
            Authentication authentication,
            @RequestParam(defaultValue = "0")
            int page,
            @RequestParam(defaultValue = "10")
            int size
    ) {

        User user =
                userRepository.findByUsername(
                        authentication.getName()
                ).orElseThrow(() ->
                        new IllegalStateException(
                                "Authenticated user no longer exists"
                        )
                );

        Pageable pageable =
                PageRequest.of(
                        page,
                        size
                );

        Page<Audit> history =
                auditRepository
                        .findByUserOrderByTimestampDesc(
                                user,
                                pageable
                        );

        Page<AuditHistoryResponse> response =
                history.map(audit ->
                        new AuditHistoryResponse(
                                audit.getId(),
                                audit.getModule(),
                                audit.getThreatLevel(),
                                audit.getRiskScore(),
                                audit.getDetails(),
                                audit.getTimestamp(),
                                audit.getResultData()
                        )
                );

        return ResponseEntity.ok(response);
    }
}