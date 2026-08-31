package com.swifteats.tracking.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;

@ConfigurationProperties(prefix = "swifteats.tracking")
public class TrackingProperties {

    private boolean kafkaEnabled = true;
    private Duration locationCacheTtl = Duration.ofMinutes(30);
    private int archiveSampleIntervalSec = 30;
    private long sseTimeoutMs = 300_000L;
    private int sseHeartbeatSec = 15;
    private boolean simulatorEnabled = false;
    private int simulatorDriverCount = 5;
    private long simulatorIntervalMs = 5_000L;

    public boolean isKafkaEnabled() {
        return kafkaEnabled;
    }

    public void setKafkaEnabled(boolean kafkaEnabled) {
        this.kafkaEnabled = kafkaEnabled;
    }

    public Duration getLocationCacheTtl() {
        return locationCacheTtl;
    }

    public void setLocationCacheTtl(Duration locationCacheTtl) {
        this.locationCacheTtl = locationCacheTtl;
    }

    public int getArchiveSampleIntervalSec() {
        return archiveSampleIntervalSec;
    }

    public void setArchiveSampleIntervalSec(int archiveSampleIntervalSec) {
        this.archiveSampleIntervalSec = archiveSampleIntervalSec;
    }

    public long getSseTimeoutMs() {
        return sseTimeoutMs;
    }

    public void setSseTimeoutMs(long sseTimeoutMs) {
        this.sseTimeoutMs = sseTimeoutMs;
    }

    public int getSseHeartbeatSec() {
        return sseHeartbeatSec;
    }

    public void setSseHeartbeatSec(int sseHeartbeatSec) {
        this.sseHeartbeatSec = sseHeartbeatSec;
    }

    public boolean isSimulatorEnabled() {
        return simulatorEnabled;
    }

    public void setSimulatorEnabled(boolean simulatorEnabled) {
        this.simulatorEnabled = simulatorEnabled;
    }

    public int getSimulatorDriverCount() {
        return simulatorDriverCount;
    }

    public void setSimulatorDriverCount(int simulatorDriverCount) {
        this.simulatorDriverCount = simulatorDriverCount;
    }

    public long getSimulatorIntervalMs() {
        return simulatorIntervalMs;
    }

    public void setSimulatorIntervalMs(long simulatorIntervalMs) {
        this.simulatorIntervalMs = simulatorIntervalMs;
    }
}
