package com.byakushield.backend.service;

import com.byakushield.backend.dto.OrchestratorRequest;
import com.byakushield.backend.dto.OrchestratorResponse;
import com.byakushield.backend.dto.QuishGuardRequest;
import com.byakushield.backend.dto.TextArmorRequest;
import com.byakushield.backend.dto.ThreatResponse;

import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

@Service
public class OrchestratorService {

    private final TextArmorService textArmorService;
    private final QuishGuardService quishGuardService;
    private final VoiceShieldService voiceShieldService;
    private final DataScrubService dataScrubService;
    private final AuditService auditService;

    public OrchestratorService(
            TextArmorService textArmorService,
            QuishGuardService quishGuardService,
            VoiceShieldService voiceShieldService,
            DataScrubService dataScrubService,
            AuditService auditService
    ) {
        this.textArmorService = textArmorService;
        this.quishGuardService = quishGuardService;
        this.voiceShieldService = voiceShieldService;
        this.dataScrubService = dataScrubService;
        this.auditService = auditService;
    }

    public OrchestratorResponse analyze(
            OrchestratorRequest request,
            MultipartFile audio,
            MultipartFile media,
            MultipartFile qr,
            String username
    ) {

        List<CompletableFuture<ThreatResponse>> futures =
                new ArrayList<>();

        /*
         * TextArmor
         */
        if (request.getText() != null
                && !request.getText().isBlank()) {

            TextArmorRequest textRequest =
                    new TextArmorRequest();

            textRequest.setMessage(
                    request.getText()
            );

            futures.add(
                    CompletableFuture.supplyAsync(() ->
                            analyzeSafely(
                                    "TextArmor",
                                    () ->
                                            textArmorService.analyze(
                                                    textRequest
                                            )
                            )
                    )
            );
        }

        /*
         * QuishGuard - URL
         */
        if (request.getUrl() != null
                && !request.getUrl().isBlank()) {

            QuishGuardRequest quishRequest =
                    new QuishGuardRequest();

            quishRequest.setUrl(
                    request.getUrl()
            );

            futures.add(
                    CompletableFuture.supplyAsync(() ->
                            analyzeSafely(
                                    "QuishGuard",
                                    () ->
                                            quishGuardService.analyze(
                                                    quishRequest
                                            )
                            )
                    )
            );
        }

        /*
         * QuishGuard - QR
         */
        if (qr != null && !qr.isEmpty()) {

            futures.add(
                    CompletableFuture.supplyAsync(() ->
                            analyzeSafely(
                                    "QuishGuard",
                                    () ->
                                            quishGuardService.analyzeQr(
                                                    qr
                                            )
                            )
                    )
            );
        }

        /*
         * VoiceShield
         */
        if (audio != null && !audio.isEmpty()) {

            futures.add(
                    CompletableFuture.supplyAsync(() ->
                            analyzeSafely(
                                    "VoiceShield",
                                    () ->
                                            voiceShieldService.analyze(
                                                    audio
                                            )
                            )
                    )
            );
        }

        /*
         * DataScrub
         *
         * DataScrub now returns the same ThreatResponse
         * type used by every other security module.
         */
        CompletableFuture<String> sanitizedDataFuture =
                CompletableFuture.completedFuture(null);

        if (media != null && !media.isEmpty()) {

            futures.add(
                    CompletableFuture.supplyAsync(() ->
                            analyzeSafely(
                                    "DataScrub",
                                    () ->
                                            dataScrubService.scrub(
                                                    media
                                            )
                            )
                    )
            );

            /*
             * Generate the sanitized image separately so
             * it can be returned in the orchestrator report.
             */
            sanitizedDataFuture =
                    CompletableFuture.supplyAsync(() -> {

                        try {

                            return dataScrubService
                                    .createSanitizedData(
                                            media.getBytes()
                                    );

                        } catch (Exception e) {

                            return null;
                        }
                    });
        }

        /*
         * Wait for all active security modules.
         */
        List<ThreatResponse> results =
                futures.stream()
                        .map(CompletableFuture::join)
                        .toList();

        /*
         * Retrieve sanitized image data.
         */
        String sanitizedData =
                sanitizedDataFuture.join();

        /*
         * Calculate aggregate risk score.
         */
        double riskScore =
                calculateWeightedRiskScore(
                        results
                );

        /*
         * Calculate overall verdict.
         */
        String verdict =
                calculateVerdict(
                        riskScore
                );

        /*
         * Build final structured response.
         */
        OrchestratorResponse response =
                new OrchestratorResponse(
                        verdict,
                        riskScore,
                        results,
                        sanitizedData
                );

        /*
         * Persist aggregate audit.
         */
        auditService.saveOrchestratorAudit(
                username,
                response
        );

        return response;
    }

    /**
     * Executes an individual security module safely.
     *
     * If a module fails unexpectedly, an ERROR result is
     * returned instead of terminating the entire scan.
     */
    private ThreatResponse analyzeSafely(
            String moduleName,
            ModuleAnalyzer analyzer
    ) {

        try {

            ThreatResponse response =
                    analyzer.analyze();

            if (response == null) {

                return new ThreatResponse(
                        moduleName,
                        "ERROR",
                        0.0,
                        moduleName +
                                " analysis returned no result."
                );
            }

            return response;

        } catch (Exception e) {

            return new ThreatResponse(
                    moduleName,
                    "ERROR",
                    0.0,
                    moduleName +
                            " analysis failed safely. " +
                            "The remaining security modules " +
                            "continued normally."
            );
        }
    }

    /**
     * Functional interface used to execute individual
     * security-module analysis operations.
     */
    @FunctionalInterface
    private interface ModuleAnalyzer {

        ThreatResponse analyze();
    }

    /**
     * Calculates the aggregate weighted risk score.
     *
     * Current Phase 3 framework uses equal weighting
     * because no official unequal module weights were
     * defined in the project requirements.
     */
    private double calculateWeightedRiskScore(
            List<ThreatResponse> results
    ) {

        if (results.isEmpty()) {
            return 0.0;
        }

        double weightedTotal = 0.0;
        double totalWeight = 0.0;

        for (ThreatResponse result :
                results) {

            double weight = 1.0;

            weightedTotal +=
                    result.getRiskScore() *
                            weight;

            totalWeight += weight;
        }

        double score =
                weightedTotal /
                        totalWeight;

        return Math.round(
                Math.min(
                        score,
                        1.0
                ) * 100.0
        ) / 100.0;
    }

    /**
     * Calculates the final overall verdict.
     */
    private String calculateVerdict(
            double riskScore
    ) {

        if (riskScore >= 0.50) {
            return "HIGH";
        }

        if (riskScore >= 0.25) {
            return "MEDIUM";
        }

        return "LOW";
    }
}