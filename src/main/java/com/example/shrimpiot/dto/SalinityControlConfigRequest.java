package com.example.shrimpiot.dto;

public class SalinityControlConfigRequest {
    private Boolean salinityAutoEnabled;
    private Double salinityHighThreshold;
    private Double salinityStopThreshold;
    private Integer salinityDrainDurationSeconds;
    private Integer freshwaterDurationSeconds;
    private Integer mixingWaitSeconds;
    private Integer maxRetryCount;
    private Integer cooldownMinutes;
    private Integer readingMaxAgeSeconds;
    private Boolean autoRemeasureEnabled;
    private Boolean safetyLockEnabled;

    public Boolean getSalinityAutoEnabled() { return salinityAutoEnabled; }
    public void setSalinityAutoEnabled(Boolean salinityAutoEnabled) { this.salinityAutoEnabled = salinityAutoEnabled; }
    public Double getSalinityHighThreshold() { return salinityHighThreshold; }
    public void setSalinityHighThreshold(Double salinityHighThreshold) { this.salinityHighThreshold = salinityHighThreshold; }
    public Double getSalinityStopThreshold() { return salinityStopThreshold; }
    public void setSalinityStopThreshold(Double salinityStopThreshold) { this.salinityStopThreshold = salinityStopThreshold; }
    public Integer getSalinityDrainDurationSeconds() { return salinityDrainDurationSeconds; }
    public void setSalinityDrainDurationSeconds(Integer salinityDrainDurationSeconds) { this.salinityDrainDurationSeconds = salinityDrainDurationSeconds; }
    public Integer getFreshwaterDurationSeconds() { return freshwaterDurationSeconds; }
    public void setFreshwaterDurationSeconds(Integer freshwaterDurationSeconds) { this.freshwaterDurationSeconds = freshwaterDurationSeconds; }
    public Integer getMixingWaitSeconds() { return mixingWaitSeconds; }
    public void setMixingWaitSeconds(Integer mixingWaitSeconds) { this.mixingWaitSeconds = mixingWaitSeconds; }
    public Integer getMaxRetryCount() { return maxRetryCount; }
    public void setMaxRetryCount(Integer maxRetryCount) { this.maxRetryCount = maxRetryCount; }
    public Integer getCooldownMinutes() { return cooldownMinutes; }
    public void setCooldownMinutes(Integer cooldownMinutes) { this.cooldownMinutes = cooldownMinutes; }
    public Integer getReadingMaxAgeSeconds() { return readingMaxAgeSeconds; }
    public void setReadingMaxAgeSeconds(Integer readingMaxAgeSeconds) { this.readingMaxAgeSeconds = readingMaxAgeSeconds; }
    public Boolean getAutoRemeasureEnabled() { return autoRemeasureEnabled; }
    public void setAutoRemeasureEnabled(Boolean autoRemeasureEnabled) { this.autoRemeasureEnabled = autoRemeasureEnabled; }
    public Boolean getSafetyLockEnabled() { return safetyLockEnabled; }
    public void setSafetyLockEnabled(Boolean safetyLockEnabled) { this.safetyLockEnabled = safetyLockEnabled; }
}
