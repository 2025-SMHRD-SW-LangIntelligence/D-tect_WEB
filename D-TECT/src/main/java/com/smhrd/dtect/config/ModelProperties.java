package com.smhrd.dtect.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.model")
public class ModelProperties {
    private String baseUrl = "";          // 🔴 비워두면 스텁 모드
    private String predictPath = "/predict";
    private String apiKey = "";           // 필요시 사용
    private int connectTimeoutMs = 5000;
    private int readTimeoutMs = 60000;

    // getters & setters
    public String getBaseUrl() { return baseUrl; }
    public void setBaseUrl(String baseUrl) { this.baseUrl = baseUrl; }
    public String getPredictPath() { return predictPath; }
    public void setPredictPath(String predictPath) { this.predictPath = predictPath; }
    public String getApiKey() { return apiKey; }
    public void setApiKey(String apiKey) { this.apiKey = apiKey; }
    public int getConnectTimeoutMs() { return connectTimeoutMs; }
    public void setConnectTimeoutMs(int connectTimeoutMs) { this.connectTimeoutMs = connectTimeoutMs; }
    public int getReadTimeoutMs() { return readTimeoutMs; }
    public void setReadTimeoutMs(int readTimeoutMs) { this.readTimeoutMs = readTimeoutMs; }
}
