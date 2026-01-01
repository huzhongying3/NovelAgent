package com.hxs.novelagent.worldgenesis.config;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "worldgenesis")
public class WorldGenesisProperties {

    private int sampleSize = 40;
    private int maxContextChars = 12000;
    private List<String> conflictKeywords = new ArrayList<>(Arrays.asList("死", "杀", "战", "血", "毒", "逃"));
    private String defaultOutput = "source_world_bible.json";
    private String defaultDataFile = "records-15.json";

    public int getSampleSize() {
        return sampleSize;
    }

    public void setSampleSize(int sampleSize) {
        this.sampleSize = sampleSize;
    }

    public int getMaxContextChars() {
        return maxContextChars;
    }

    public void setMaxContextChars(int maxContextChars) {
        this.maxContextChars = maxContextChars;
    }

    public List<String> getConflictKeywords() {
        return conflictKeywords;
    }

    public void setConflictKeywords(List<String> conflictKeywords) {
        this.conflictKeywords = conflictKeywords == null ? new ArrayList<>() : new ArrayList<>(conflictKeywords);
    }

    public String getDefaultOutput() {
        return defaultOutput;
    }

    public void setDefaultOutput(String defaultOutput) {
        this.defaultOutput = defaultOutput;
    }

    public String getDefaultDataFile() {
        return defaultDataFile;
    }

    public void setDefaultDataFile(String defaultDataFile) {
        this.defaultDataFile = defaultDataFile;
    }
}
