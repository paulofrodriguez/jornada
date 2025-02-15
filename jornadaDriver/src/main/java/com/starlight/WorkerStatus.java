package com.starlight;


import jakarta.json.bind.annotation.JsonbProperty;

public class WorkerStatus {
    @JsonbProperty("worker_id")
    private String workerId;

    @JsonbProperty("worker_name")
    private String workerName;

    @JsonbProperty("worker_type")
    private String workerType;

    private String status;
    private String timestamp;

    // Getters e Setters
    public String getWorkerId() {
        return workerId;
    }

    public void setWorkerId(String workerId) {
        this.workerId = workerId;
    }

    public String getWorkerName() {
        return workerName;
    }

    public void setWorkerName(String workerName) {
        this.workerName = workerName;
    }

    public String getWorkerType() {
        return workerType;
    }

    public void setWorkerType(String workerType) {
        this.workerType = workerType;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(String timestamp) {
        this.timestamp = timestamp;
    }

    public void setHost(String host) {
        this.host = host;
    }

    public void setPort(Integer timestamp) {
        this.host = host;
    }
}