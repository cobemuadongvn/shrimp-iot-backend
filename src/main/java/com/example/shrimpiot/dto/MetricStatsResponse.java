package com.example.shrimpiot.dto;

public class MetricStatsResponse {
    private String name;
    private Double min;
    private Double max;
    private Double average;
    private Double latest;

    public MetricStatsResponse(String name, Double min, Double max, Double average, Double latest) {
        this.name = name;
        this.min = min;
        this.max = max;
        this.average = average;
        this.latest = latest;
    }

    public String getName() { return name; }
    public Double getMin() { return min; }
    public Double getMax() { return max; }
    public Double getAverage() { return average; }
    public Double getLatest() { return latest; }
}
