package com.example.shrimpiot.dto;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public class AiPredictionResponse {
    @JsonAlias({"rule_status", "ruleStatus"})
    private String ruleStatus;

    @JsonAlias({"rule_message", "ruleMessage"})
    private String ruleMessage;

    @JsonAlias({"isolation_forest_status", "isolationForestStatus", "anomaly_status", "anomalyStatus"})
    private String isolationForestStatus;

    @JsonAlias({"xgboost_status", "xgboostStatus", "ml_status", "mlStatus"})
    private String xgboostStatus;

    @JsonAlias({"random_forest_status", "randomForestStatus"})
    private String randomForestStatus;

    @JsonAlias({"ai_status", "aiStatus"})
    private String aiStatus;

    @JsonAlias({"final_status", "finalStatus"})
    private String finalStatus;

    @JsonAlias({"recommendation", "recommended_action", "recommendedAction"})
    private String recommendation;

    @JsonAlias({"message", "aiMessage"})
    private String message;

    @JsonAlias({"error"})
    private String error;

    public String getRuleStatus() { return ruleStatus; }
    public void setRuleStatus(String ruleStatus) { this.ruleStatus = ruleStatus; }

    public String getRuleMessage() { return ruleMessage; }
    public void setRuleMessage(String ruleMessage) { this.ruleMessage = ruleMessage; }

    public String getIsolationForestStatus() { return isolationForestStatus; }
    public void setIsolationForestStatus(String isolationForestStatus) { this.isolationForestStatus = isolationForestStatus; }

    public String getXgboostStatus() { return xgboostStatus; }
    public void setXgboostStatus(String xgboostStatus) { this.xgboostStatus = xgboostStatus; }

    public String getRandomForestStatus() { return randomForestStatus; }
    public void setRandomForestStatus(String randomForestStatus) { this.randomForestStatus = randomForestStatus; }

    public String getAiStatus() { return aiStatus; }
    public void setAiStatus(String aiStatus) { this.aiStatus = aiStatus; }

    public String getFinalStatus() { return finalStatus; }
    public void setFinalStatus(String finalStatus) { this.finalStatus = finalStatus; }

    public String getRecommendation() { return recommendation; }
    public void setRecommendation(String recommendation) { this.recommendation = recommendation; }

    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }

    public String getError() { return error; }
    public void setError(String error) { this.error = error; }
}
