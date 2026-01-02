package com.hxs.novelagent.novelgraph.model;

public class Relationship {
    private String source;
    private String target;
    private Enums.RelationshipType type;
    private String description;
    private Integer strength;
    private Boolean isTemporary;

    public String getSource() {
        return source;
    }

    public void setSource(String source) {
        this.source = source;
    }

    public String getTarget() {
        return target;
    }

    public void setTarget(String target) {
        this.target = target;
    }

    public Enums.RelationshipType getType() {
        return type;
    }

    public void setType(Enums.RelationshipType type) {
        this.type = type;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public Integer getStrength() {
        return strength;
    }

    public void setStrength(Integer strength) {
        this.strength = strength;
    }

    public Boolean getIsTemporary() {
        return isTemporary;
    }

    public void setIsTemporary(Boolean isTemporary) {
        this.isTemporary = isTemporary;
    }
}
