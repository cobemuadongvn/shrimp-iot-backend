package com.example.shrimpiot.dto;

import jakarta.validation.constraints.NotNull;

public class ControlScenarioRequest {
    @NotNull
    private Long pondId;

    @NotNull
    private String conditionParameter;

    @NotNull
    private String operator;

    @NotNull
    private Double thresholdValue;

    @NotNull
    private Integer relayNo;

    @NotNull
    private String action;

    private Long cooldownSeconds;
    private Long maxRuntimeSeconds;
    private Boolean enabled;
    private Boolean autoOffEnabled;

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
    public String getAction() { return action; }
    public void setAction(String action) { this.action = action; }
    public Long getCooldownSeconds() { return cooldownSeconds; }
    public void setCooldownSeconds(Long cooldownSeconds) { this.cooldownSeconds = cooldownSeconds; }
    public Long getMaxRuntimeSeconds() { return maxRuntimeSeconds; }
    public void setMaxRuntimeSeconds(Long maxRuntimeSeconds) { this.maxRuntimeSeconds = maxRuntimeSeconds; }
    public Boolean getEnabled() { return enabled; }
    public void setEnabled(Boolean enabled) { this.enabled = enabled; }
    public Boolean getAutoOffEnabled() { return autoOffEnabled; }
    public void setAutoOffEnabled(Boolean autoOffEnabled) { this.autoOffEnabled = autoOffEnabled; }
}
