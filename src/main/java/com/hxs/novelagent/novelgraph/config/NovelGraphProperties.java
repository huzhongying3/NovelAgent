package com.hxs.novelagent.novelgraph.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "novelgraph")
public class NovelGraphProperties {

    private String defaultNovelPath = "/Users/yyinging/Downloads/鸳鸯刀 (金庸) (Z-Library).txt";
    private boolean resetBeforeRun = true;
    private int chunkSize = 2000;
    private int chunkOverlap = 400;
    private String model = "gemini-3-flash-preview-nothinking";
    private double temperature = 0.1;
    public String getDefaultNovelPath() {
        return defaultNovelPath;
    }

    public void setDefaultNovelPath(String defaultNovelPath) {
        this.defaultNovelPath = defaultNovelPath;
    }

    public boolean isResetBeforeRun() {
        return resetBeforeRun;
    }

    public void setResetBeforeRun(boolean resetBeforeRun) {
        this.resetBeforeRun = resetBeforeRun;
    }

    public int getChunkSize() {
        return chunkSize;
    }

    public void setChunkSize(int chunkSize) {
        this.chunkSize = chunkSize;
    }

    public int getChunkOverlap() {
        return chunkOverlap;
    }

    public void setChunkOverlap(int chunkOverlap) {
        this.chunkOverlap = chunkOverlap;
    }

    public String getModel() {
        return model;
    }

    public void setModel(String model) {
        this.model = model;
    }

    public double getTemperature() {
        return temperature;
    }

    public void setTemperature(double temperature) {
        this.temperature = temperature;
    }
}
