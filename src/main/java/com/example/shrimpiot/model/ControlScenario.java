package com.example.shrimpiot.model;

import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;

@Entity
@Table(name = "control_scenarios")
public class ControlScenario {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "pond_id", nullable = false)
    private Pond pond;

    @Column(name = "condition_parameter", nullable = false)
    private String conditionParameter; // e.g., DO, TEMPERATURE, PH

    @Column(name = "operator", nullable = false)
    private String operator; // <, >, <=, >=

    @Column(name = "threshold_value", nullable = false)
    private Double thresholdValue;

    @Column(name = "relay_no", nullable = false)
    private Integer relayNo;

    @Column(name = "action", nullable = false)
    @Enumerated(EnumType.STRING)
    private RelayAction action;

    @Column(name = "cooldown_seconds")
    private Long cooldownSeconds;

    @Column(name = "max_runtime_seconds")
    private Long maxRuntimeSeconds;

    @Column(name = "auto_off_enabled", nullable = false)
    private Boolean autoOffEnabled = true;

    @Column(name = "enabled", nullable = false)
    private Boolean enabled = true;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    public ControlScenario() {}

    @PrePersist
    protected void onCreate() {
        if (this.createdAt == null) this.createdAt = LocalDateTime.now();
        if (this.enabled == null) this.enabled = true;
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }

    // Getters and setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Pond getPond() { return pond; }
    public void setPond(Pond pond) { this.pond = pond; }
    public String getConditionParameter() { return conditionParameter; }
    public void setConditionParameter(String conditionParameter) { this.conditionParameter = conditionParameter; }
    public String getOperator() { return operator; }
    public void setOperator(String operator) { this.operator = operator; }
    public Double getThresholdValue() { return thresholdValue; }
    public void setThresholdValue(Double thresholdValue) { this.thresholdValue = thresholdValue; }
    public Integer getRelayNo() { return relayNo; }
    public void setRelayNo(Integer relayNo) { this.relayNo = relayNo; }
    public RelayAction getAction() { return action; }
    public void setAction(RelayAction action) { this.action = action; }
    public Long getCooldownSeconds() { return cooldownSeconds; }
    public void setCooldownSeconds(Long cooldownSeconds) { this.cooldownSeconds = cooldownSeconds; }
    public Long getMaxRuntimeSeconds() { return maxRuntimeSeconds; }
    public void setMaxRuntimeSeconds(Long maxRuntimeSeconds) { this.maxRuntimeSeconds = maxRuntimeSeconds; }
    public Boolean getAutoOffEnabled() { return autoOffEnabled; }
    public void setAutoOffEnabled(Boolean autoOffEnabled) { this.autoOffEnabled = autoOffEnabled; }
    public Boolean getEnabled() { return enabled; }
    public void setEnabled(Boolean enabled) { this.enabled = enabled; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
}
