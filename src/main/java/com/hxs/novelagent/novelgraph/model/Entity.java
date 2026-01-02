package com.hxs.novelagent.novelgraph.model;

import java.util.ArrayList;
import java.util.List;

public class Entity {
    private String name;
    private Enums.EntityType type;
    private Enums.EntitySubtype subtype;
    private List<String> aliases = new ArrayList<>();
    private Enums.HealthStatus healthStatus = Enums.HealthStatus.Unknown;
    private List<String> affiliation = new ArrayList<>();
    private String description;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Enums.EntityType getType() {
        return type;
    }

    public void setType(Enums.EntityType type) {
        this.type = type;
    }

    public Enums.EntitySubtype getSubtype() {
        return subtype;
    }

    public void setSubtype(Enums.EntitySubtype subtype) {
        this.subtype = subtype;
    }

    public List<String> getAliases() {
        return aliases;
    }

    public void setAliases(List<String> aliases) {
        this.aliases = aliases == null ? new ArrayList<>() : new ArrayList<>(aliases);
    }

    public Enums.HealthStatus getHealthStatus() {
        return healthStatus;
    }

    public void setHealthStatus(Enums.HealthStatus healthStatus) {
        this.healthStatus = healthStatus;
    }

    public List<String> getAffiliation() {
        return affiliation;
    }

    public void setAffiliation(List<String> affiliation) {
        this.affiliation = affiliation == null ? new ArrayList<>() : new ArrayList<>(affiliation);
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }
}
