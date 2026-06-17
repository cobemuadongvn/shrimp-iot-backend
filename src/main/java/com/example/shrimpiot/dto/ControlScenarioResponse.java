package com.example.shrimpiot.dto;

import java.time.LocalDateTime;

import com.example.shrimpiot.model.ControlScenario;
import com.example.shrimpiot.model.RelayAction;

public class ControlScenarioResponse {
    private Long id;
    private Long pondId;
    private String conditionParameter;
    private String operator;
    private Double thresholdValue;
    private Integer relayNo;
    private RelayAction action;
    private Long cooldownSeconds;
    private Long maxRuntimeSeconds;
    private Boolean enabled;
    private Boolean autoOffEnabled;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public ControlScenarioResponse() {}

    public ControlScenarioResponse(ControlScenario scenario) {
        this.id = scenario.getId();
        this.pondId = scenario.getPond() != null ? scenario.getPond().getId() : null;
        this.conditionParameter = scenario.getConditionParameter();
        this.operator = scenario.getOperator();
        this.thresholdValue = scenario.getThresholdValue();
        this.relayNo = scenario.getRelayNo();
        this.action = scenario.getAction();
        this.cooldownSeconds = scenario.getCooldownSeconds();
        this.maxRuntimeSeconds = scenario.getMaxRuntimeSeconds();
        this.enabled = scenario.getEnabled();
        this.autoOffEnabled = scenario.getAutoOffEnabled();
        this.createdAt = scenario.getCreatedAt();
        this.updatedAt = scenario.getUpdatedAt();
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getPondId() { return pondId; }
    public void setPondId(Long pondId) { this.pondId = pondId; }
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
    public Boolean getEnabled() { return enabled; }
    public void setEnabled(Boolean enabled) { this.enabled = enabled; }
    public Boolean getAutoOffEnabled() { return autoOffEnabled; }
    public void setAutoOffEnabled(Boolean autoOffEnabled) { this.autoOffEnabled = autoOffEnabled; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
}
