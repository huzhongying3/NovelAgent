package com.hxs.novelagent.novelgraph.model;

public class Enums {

    public enum EntityType {
        Person, Location, Organization, Item, Concept
    }

    public enum EntitySubtype {
        MainCharacter, SupportingCharacter, Antagonist, Mob, Weapon, Medicine, Book, Tool, Treasure, City,
        Wilderness, Building, Room, Sect, Government, Family, Unknown
    }

    public enum HealthStatus {
        Healthy, Injured, Critical, Deceased, Poisoned, Unknown
    }

    public enum RelationshipType {
        ATTACKED, PROTECTED, KILLED, SAVED, KNOWS, TALKED_TO, RELATED_TO, LOVES, HATES, MASTER_OF,
        STUDENT_OF, MEMBER_OF, OWNS, USED, LOST, LOCATED_AT, TRAVELLED_TO
    }

    public enum TimeOfDay {
        Morning, Noon, Afternoon, Evening, Night, Unknown
    }
}
