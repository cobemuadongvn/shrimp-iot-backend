package com.example.shrimpiot.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import com.fasterxml.jackson.annotation.JsonAlias;

public class SensorReadingRequest {

    @NotBlank(message = "deviceId is required")
    @JsonAlias({"device_id", "node_code"})
    private String deviceId;

    @NotNull(message = "temperature is required")
    @JsonAlias({"temp", "water_temperature"})
    private Double temperature;

    @NotNull(message = "ph is required")
    private Double ph;

    @JsonAlias({"ec_value", "ec"})
    private Double ecValue;

    @NotNull(message = "salinity is required")
    private Double salinity;

    @JsonAlias({"do_value", "dissolved_oxygen", "do"})
    private Double doValue;

    public String getDeviceId() {
        return deviceId;
    }

    public void setDeviceId(String deviceId) {
        this.deviceId = deviceId;
    }

    public Double getTemperature() {
        return temperature;
    }

    public void setTemperature(Double temperature) {
        this.temperature = temperature;
    }

    public Double getPh() {
        return ph;
    }

    public void setPh(Double ph) {
        this.ph = ph;
    }

    public Double getEcValue() {
        return ecValue;
    }

    public void setEcValue(Double ecValue) {
        this.ecValue = ecValue;
    }

    public Double getSalinity() {
        return salinity;
    }

    public void setSalinity(Double salinity) {
        this.salinity = salinity;
    }

    public Double getDoValue() {
        return doValue;
    }

    public void setDoValue(Double doValue) {
        this.doValue = doValue;
    }
}
