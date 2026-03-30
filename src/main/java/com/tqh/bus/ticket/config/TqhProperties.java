package com.tqh.bus.ticket.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "tqh")
public class TqhProperties {

    private String baseUrl;
    private String authToken;
    private int routeId;
    private int boardingPointId;
    private int alightingPointId;
    private int monitorInterval;

    // 日志配置
    private String logFilePath = "./logs/ticket.log";
    private String logClearCron = "0 0 10 ? * FRI";

    // API 查询配置
    private int defaultOrderPage = 1;
    private int defaultOrderPageSize = 10;

    public String getBaseUrl() {
        return baseUrl;
    }

    public void setBaseUrl(String baseUrl) {
        this.baseUrl = baseUrl;
    }

    public String getAuthToken() {
        return authToken;
    }

    public void setAuthToken(String authToken) {
        this.authToken = authToken;
    }

    public int getRouteId() {
        return routeId;
    }

    public void setRouteId(int routeId) {
        this.routeId = routeId;
    }

    public int getBoardingPointId() {
        return boardingPointId;
    }

    public void setBoardingPointId(int boardingPointId) {
        this.boardingPointId = boardingPointId;
    }

    public int getAlightingPointId() {
        return alightingPointId;
    }

    public void setAlightingPointId(int alightingPointId) {
        this.alightingPointId = alightingPointId;
    }

    public int getMonitorInterval() {
        return monitorInterval;
    }

    public void setMonitorInterval(int monitorInterval) {
        this.monitorInterval = monitorInterval;
    }

    public String getLogFilePath() {
        return logFilePath;
    }

    public void setLogFilePath(String logFilePath) {
        this.logFilePath = logFilePath;
    }

    public String getLogClearCron() {
        return logClearCron;
    }

    public void setLogClearCron(String logClearCron) {
        this.logClearCron = logClearCron;
    }

    public int getDefaultOrderPage() {
        return defaultOrderPage;
    }

    public void setDefaultOrderPage(int defaultOrderPage) {
        this.defaultOrderPage = defaultOrderPage;
    }

    public int getDefaultOrderPageSize() {
        return defaultOrderPageSize;
    }

    public void setDefaultOrderPageSize(int defaultOrderPageSize) {
        this.defaultOrderPageSize = defaultOrderPageSize;
    }
}
