package com.hxs.novelagent.worldgenesis.model;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import com.fasterxml.jackson.annotation.JsonProperty;

public class WorldBible {

    @JsonProperty("power_system")
    private String powerSystem;

    @JsonProperty("power_limitations")
    private String powerLimitations;

    @JsonProperty("geographical_scope")
    private String geographicalScope;

    @JsonProperty("social_hierarchy")
    private String socialHierarchy;

    @JsonProperty("world_conflict_source")
    private String worldConflictSource;

    @JsonProperty("major_factions")
    private List<String> majorFactions = new ArrayList<>();

    @JsonProperty("economic_rules")
    private String economicRules;

    @JsonProperty("cultural_taboos")
    private String culturalTaboos;

    @JsonProperty("specific_terminology")
    private List<String> specificTerminology = new ArrayList<>();

    public WorldBible() {
    }

    public WorldBible(
            String powerSystem,
            String powerLimitations,
            String geographicalScope,
            String socialHierarchy,
            String worldConflictSource,
            List<String> majorFactions,
            String economicRules,
            String culturalTaboos,
            List<String> specificTerminology
    ) {
        this.powerSystem = powerSystem;
        this.powerLimitations = powerLimitations;
        this.geographicalScope = geographicalScope;
        this.socialHierarchy = socialHierarchy;
        this.worldConflictSource = worldConflictSource;
        if (majorFactions != null) {
            this.majorFactions = new ArrayList<>(majorFactions);
        }
        this.economicRules = economicRules;
        this.culturalTaboos = culturalTaboos;
        if (specificTerminology != null) {
            this.specificTerminology = new ArrayList<>(specificTerminology);
        }
    }

    public String getPowerSystem() {
        return powerSystem;
    }

    public void setPowerSystem(String powerSystem) {
        this.powerSystem = powerSystem;
    }

    public String getPowerLimitations() {
        return powerLimitations;
    }

    public void setPowerLimitations(String powerLimitations) {
        this.powerLimitations = powerLimitations;
    }

    public String getGeographicalScope() {
        return geographicalScope;
    }

    public void setGeographicalScope(String geographicalScope) {
        this.geographicalScope = geographicalScope;
    }

    public String getSocialHierarchy() {
        return socialHierarchy;
    }

    public void setSocialHierarchy(String socialHierarchy) {
        this.socialHierarchy = socialHierarchy;
    }

    public String getWorldConflictSource() {
        return worldConflictSource;
    }

    public void setWorldConflictSource(String worldConflictSource) {
        this.worldConflictSource = worldConflictSource;
    }

    public List<String> getMajorFactions() {
        return majorFactions;
    }

    public void setMajorFactions(List<String> majorFactions) {
        this.majorFactions = majorFactions == null ? new ArrayList<>() : new ArrayList<>(majorFactions);
    }

    public String getEconomicRules() {
        return economicRules;
    }

    public void setEconomicRules(String economicRules) {
        this.economicRules = economicRules;
    }

    public String getCulturalTaboos() {
        return culturalTaboos;
    }

    public void setCulturalTaboos(String culturalTaboos) {
        this.culturalTaboos = culturalTaboos;
    }

    public List<String> getSpecificTerminology() {
        return specificTerminology;
    }

    public void setSpecificTerminology(List<String> specificTerminology) {
        this.specificTerminology = specificTerminology == null ? new ArrayList<>() : new ArrayList<>(specificTerminology);
    }

    @Override
    public String toString() {
        return "WorldBible{" +
                "powerSystem='" + powerSystem + '\'' +
                ", powerLimitations='" + powerLimitations + '\'' +
                ", geographicalScope='" + geographicalScope + '\'' +
                ", socialHierarchy='" + socialHierarchy + '\'' +
                ", worldConflictSource='" + worldConflictSource + '\'' +
                ", majorFactions=" + majorFactions +
                ", economicRules='" + economicRules + '\'' +
                ", culturalTaboos='" + culturalTaboos + '\'' +
                ", specificTerminology=" + specificTerminology +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        WorldBible that = (WorldBible) o;
        return Objects.equals(powerSystem, that.powerSystem)
                && Objects.equals(powerLimitations, that.powerLimitations)
                && Objects.equals(geographicalScope, that.geographicalScope)
                && Objects.equals(socialHierarchy, that.socialHierarchy)
                && Objects.equals(worldConflictSource, that.worldConflictSource)
                && Objects.equals(majorFactions, that.majorFactions)
                && Objects.equals(economicRules, that.economicRules)
                && Objects.equals(culturalTaboos, that.culturalTaboos)
                && Objects.equals(specificTerminology, that.specificTerminology);
    }

    @Override
    public int hashCode() {
        return Objects.hash(
                powerSystem,
                powerLimitations,
                geographicalScope,
                socialHierarchy,
                worldConflictSource,
                majorFactions,
                economicRules,
                culturalTaboos,
                specificTerminology
        );
    }
}
