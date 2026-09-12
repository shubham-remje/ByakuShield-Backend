package com.byakushield.backend.model;

import jakarta.persistence.*;

import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.LocalDateTime;
import java.util.Map;

@Entity
@Table(
        name = "audit_logs",
        indexes = {
                @Index(name = "idx_audit_user_id", columnList = "user_id"),
                @Index(name = "idx_audit_timestamp", columnList = "timestamp"),
                @Index(name = "idx_audit_module", columnList = "module")
        }
)
public class Audit {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /*
     * Each audit record belongs to a registered user.
     * This creates the users.id -> audit_logs.user_id relationship.
     */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(
            name = "user_id",
            nullable = false,
            foreignKey = @ForeignKey(name = "fk_audit_user")
    )
    private User user;

    /*
     * Username is retained as part of the audit record.
     * This provides a snapshot of the username at the time
     * the audit was created.
     */
    @Column(nullable = false, length = 50)
    private String username;

    @Column(nullable = false, length = 50)
    private String module;

    @Column(nullable = false, length = 20)
    private String threatLevel;

    @Column(nullable = false)
    private double riskScore;

    @Column(length = 2000)
    private String details;

    @Column(nullable = false)
    private LocalDateTime timestamp;

    /**
     * Dynamic audit information stored as PostgreSQL JSONB.
     *
     * Examples:
     * - detected indicators
     * - module findings
     * - input metadata
     * - sanitized metadata
     * - processing information
     */
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(
            name = "result_data",
            columnDefinition = "jsonb"
    )
    private Map<String, Object> resultData;

    public Audit() {
    }

    public Audit(
            User user,
            String module,
            String threatLevel,
            double riskScore,
            String details,
            Map<String, Object> resultData
    ) {
        this.user = user;
        this.username = user.getUsername();
        this.module = module;
        this.threatLevel = threatLevel;
        this.riskScore = riskScore;
        this.details = details;
        this.resultData = resultData;
        this.timestamp = LocalDateTime.now();
    }

    @PrePersist
    protected void onCreate() {

        if (timestamp == null) {
            timestamp = LocalDateTime.now();
        }

        if (user != null && username == null) {
            username = user.getUsername();
        }
    }

    public Long getId() {
        return id;
    }

    public User getUser() {
        return user;
    }

    public String getUsername() {
        return username;
    }

    public String getModule() {
        return module;
    }

    public String getThreatLevel() {
        return threatLevel;
    }

    public double getRiskScore() {
        return riskScore;
    }

    public String getDetails() {
        return details;
    }

    public LocalDateTime getTimestamp() {
        return timestamp;
    }

    public Map<String, Object> getResultData() {
        return resultData;
    }

    public void setResultData(Map<String, Object> resultData) {
        this.resultData = resultData;
    }
}