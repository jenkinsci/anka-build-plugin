package com.veertu;

public class WeighedURL {
    private final String url;
    private  int weight;
    private int latency;
    private boolean failed;

    public WeighedURL(String url) {
        this.url = url;
        this.latency = 1;
        this.failed = false;
        this.weight = RoundRobin.startWeight;
    }

    public String getUrl() {
        return this.url;
    }

    public int getWeight() {
        return this.weight;
    }

    public int getLatency() {
        return this.latency;
    }

    public boolean isFailed() {
        return this.failed;
    }

    public void setWeight(int startWeight) {
        this.weight = startWeight;
    }

    public void setFailed(boolean b) {
        this.failed = b;
    }

    public void setLatency(int latency) {
        this.latency = latency;
    }
}
