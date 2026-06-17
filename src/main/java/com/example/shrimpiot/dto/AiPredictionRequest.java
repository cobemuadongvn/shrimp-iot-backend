package com.example.shrimpiot.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public class AiPredictionRequest {
    private Double temperature;
    private Double ph;

    @JsonProperty("ec_value")
    private Double ecValue;

    private Double salinity;

    @JsonProperty("do_value")
    private Double doValue;

    public AiPredictionRequest() {
    }

    public AiPredictionRequest(Double temperature, Double ph, Double ecValue, Double salinity, Double doValue) {
        this.temperature = temperature;
        this.ph = ph;
        this.ecValue = ecValue;
        this.salinity = salinity;
        this.doValue = doValue;
    }

    public Double getTemperature() { return temperature; }
    public Double getPh() { return ph; }
    @JsonProperty("ec_value")
    public Double getEcValue() { return ecValue; }
    public Double getSalinity() { return salinity; }
    @JsonProperty("do_value")
    public Double getDoValue() { return doValue; }
}
