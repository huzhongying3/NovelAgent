package com.hxs.novelagent.novelgraph.model;

import java.util.ArrayList;
import java.util.List;

public class PlotEvent {
    private Integer orderId;
    private String summary;
    private List<String> participants = new ArrayList<>();
    private Enums.TimeOfDay timeOfDay = Enums.TimeOfDay.Unknown;
    private Integer daySequence = 1;
    private Boolean isFlashback = false;
    private String atmosphere;
    private String keyDialogue;
    private Integer timestampRelative;
    private String chunkId;

    public Integer getOrderId() {
        return orderId;
    }

    public void setOrderId(Integer orderId) {
        this.orderId = orderId;
    }

    public String getSummary() {
        return summary;
    }

    public void setSummary(String summary) {
        this.summary = summary;
    }

    public List<String> getParticipants() {
        return participants;
    }

    public void setParticipants(List<String> participants) {
        this.participants = participants == null ? new ArrayList<>() : new ArrayList<>(participants);
    }

    public Enums.TimeOfDay getTimeOfDay() {
        return timeOfDay;
    }

    public void setTimeOfDay(Enums.TimeOfDay timeOfDay) {
        this.timeOfDay = timeOfDay;
    }

    public Integer getDaySequence() {
        return daySequence;
    }

    public void setDaySequence(Integer daySequence) {
        this.daySequence = daySequence;
    }

    public Boolean getIsFlashback() {
        return isFlashback;
    }

    public void setIsFlashback(Boolean isFlashback) {
        this.isFlashback = isFlashback;
    }

    public String getAtmosphere() {
        return atmosphere;
    }

    public void setAtmosphere(String atmosphere) {
        this.atmosphere = atmosphere;
    }

    public String getKeyDialogue() {
        return keyDialogue;
    }

    public void setKeyDialogue(String keyDialogue) {
        this.keyDialogue = keyDialogue;
    }

    public Integer getTimestampRelative() {
        return timestampRelative;
    }

    public void setTimestampRelative(Integer timestampRelative) {
        this.timestampRelative = timestampRelative;
    }

    public String getChunkId() {
        return chunkId;
    }

    public void setChunkId(String chunkId) {
        this.chunkId = chunkId;
    }
}
