package com.example.shrimpiot.model;

public enum SalinityCorrectionStatus {
    SALINITY_HIGH_DETECTED,
    DRAINING_SALTY_WATER,
    ADDING_FRESH_WATER,
    WAITING_MIXING,
    RECHECKING,
    COMPLETED,
    NEED_MANUAL_CHECK,
    ERROR
}
