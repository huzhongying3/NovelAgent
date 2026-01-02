package com.hxs.novelagent.novelgraph.model;

import java.util.ArrayList;
import java.util.List;

public class ExtractionResult {

    private ChunkSummary chunkSummary = new ChunkSummary();
    private List<Entity> entities = new ArrayList<>();
    private List<Relationship> relationships = new ArrayList<>();
    private List<PlotEvent> events = new ArrayList<>();
    private String nextSummary;
    private List<String> activeCharacters = new ArrayList<>();

    public ChunkSummary getChunkSummary() {
        return chunkSummary;
    }

    public void setChunkSummary(ChunkSummary chunkSummary) {
        this.chunkSummary = chunkSummary;
    }

    public List<Entity> getEntities() {
        return entities;
    }

    public void setEntities(List<Entity> entities) {
        this.entities = entities == null ? new ArrayList<>() : new ArrayList<>(entities);
    }

    public List<Relationship> getRelationships() {
        return relationships;
    }

    public void setRelationships(List<Relationship> relationships) {
        this.relationships = relationships == null ? new ArrayList<>() : new ArrayList<>(relationships);
    }

    public List<PlotEvent> getEvents() {
        return events;
    }

    public void setEvents(List<PlotEvent> events) {
        this.events = events == null ? new ArrayList<>() : new ArrayList<>(events);
    }

    public String getNextSummary() {
        return nextSummary;
    }

    public void setNextSummary(String nextSummary) {
        this.nextSummary = nextSummary;
    }

    public List<String> getActiveCharacters() {
        return activeCharacters;
    }

    public void setActiveCharacters(List<String> activeCharacters) {
        this.activeCharacters = activeCharacters == null ? new ArrayList<>() : new ArrayList<>(activeCharacters);
    }
}
