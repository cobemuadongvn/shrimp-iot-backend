package com.example.shrimpiot.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public class CommandRequest {

    @NotBlank(message = "deviceId is required")
    private String deviceId;

    @NotNull(message = "relayNo is required")
    @Min(value = 1, message = "relayNo must be from 1 to 4")
    @Max(value = 4, message = "relayNo must be from 1 to 4")
    private Integer relayNo;

    @NotBlank(message = "action is required")
    private String action; // ON / OFF

    public String getDeviceId() {
        return deviceId;
    }

    public void setDeviceId(String deviceId) {
        this.deviceId = deviceId;
    }

    public Integer getRelayNo() {
        return relayNo;
    }

    public void setRelayNo(Integer relayNo) {
        this.relayNo = relayNo;
    }

    public String getAction() {
        return action;
    }

    public void setAction(String action) {
        this.action = action;
    }
}
