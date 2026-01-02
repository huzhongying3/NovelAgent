package com.hxs.novelagent.novelgraph.service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.hxs.novelagent.novelgraph.model.Entity;
import com.hxs.novelagent.novelgraph.model.Enums;
import com.hxs.novelagent.novelgraph.model.ExtractionResult;
import com.hxs.novelagent.novelgraph.model.PlotEvent;
import com.hxs.novelagent.novelgraph.model.Relationship;
import org.neo4j.driver.Driver;
import org.neo4j.driver.Session;
import org.neo4j.driver.TransactionContext;
import org.springframework.stereotype.Service;

@Service("novelGraphGraphService")
public class GraphService {

    private static final Pattern CJK_NAME = Pattern.compile("[\\u4e00-\\u9fff]{2,6}");

    private final Driver driver;

    public GraphService(Driver driver) {
        this.driver = driver;
    }

    public void initIndexes() {
        List<String> cyphers = List.of(
                "CREATE INDEX entity_name_idx IF NOT EXISTS FOR (n:Entity) ON (n.name)",
                "CREATE INDEX event_chunk_idx IF NOT EXISTS FOR (e:PlotEvent) ON (e.chunk_id)",
                "CREATE FULLTEXT INDEX entity_fulltext IF NOT EXISTS FOR (n:Entity) ON EACH [n.name, n.aliases, n.description]"
        );
        try (Session session = driver.session()) {
            for (String c : cyphers) {
                session.run(c);
            }
        }
    }

    public void clear() {
        try (Session session = driver.session()) {
            session.run("MATCH (n) DETACH DELETE n");
        }
    }

    public List<Map<String, Object>> fetchContextCandidates(String text, int limit) {
        List<String> tokens = extractTokens(text);
        if (tokens.isEmpty()) {
            return List.of();
        }
        String query = """
                MATCH (e:Entity)
                WHERE e.name IN $tokens OR any(a IN coalesce(e.aliases, []) WHERE a IN $tokens)
                RETURN e.name AS name, coalesce(e.aliases, []) AS aliases,
                       coalesce(e.description, '') AS description,
                       coalesce(e.subtype, 'Unknown') AS subtype,
                       coalesce(e.last_seen_chunk, -1) AS last_seen_chunk
                ORDER BY CASE WHEN e.subtype = 'MainCharacter' THEN 0 ELSE 1 END, last_seen_chunk DESC
                LIMIT $limit
                """;
        try (Session session = driver.session()) {
            return session.run(query, Map.of("tokens", tokens, "limit", limit))
                    .list(r -> Map.of(
                            "name", r.get("name").asString(""),
                            "aliases", r.get("aliases").asList(v -> v.asString("")),
                            "desc", r.get("description").asString(""),
                            "subtype", r.get("subtype").asString(""),
                            "last_seen_chunk", r.get("last_seen_chunk").asInt(-1)
                    ));
        }
    }

    public void save(ExtractionResult result) {
        try (Session session = driver.session()) {
            session.executeWriteWithoutResult(tx -> writeEntities(tx, result.getEntities()));
            session.executeWriteWithoutResult(tx -> writeRelationships(tx, result.getRelationships()));
            session.executeWriteWithoutResult(tx -> writeEvents(tx, result.getEvents()));
            if (!result.getEvents().isEmpty()) {
                String chunkId = result.getEvents().get(0).getChunkId();
                session.executeWriteWithoutResult(tx -> linkParticipants(tx, chunkId));
                session.executeWriteWithoutResult(tx -> linkTimeline(tx, chunkId));
            }
        }
    }

    private List<String> extractTokens(String text) {
        Matcher m = CJK_NAME.matcher(text);
        List<String> tokens = new ArrayList<>();
        while (m.find()) {
            String t = m.group();
            if (!tokens.contains(t)) {
                tokens.add(t);
            }
        }
        return tokens;
    }

    private void writeEntities(TransactionContext tx, List<Entity> entities) {
        String cypher = """
                UNWIND $entities AS ent
                MERGE (n:Entity {name: ent.name})
                ON CREATE SET n.aliases = ent.aliases, n.description = ent.description,
                              n.type = ent.type, n.subtype = ent.subtype,
                              n.health_status = ent.health_status, n.affiliation = ent.affiliation
                ON MATCH SET
                    n.aliases = reduce(a = [], x IN coalesce(n.aliases, []) + ent.aliases |
                        CASE WHEN x IN a OR x IS NULL THEN a ELSE a + x END),
                    n.description = coalesce(n.description, ent.description, n.description),
                    n.type = coalesce(n.type, ent.type),
                    n.subtype = coalesce(ent.subtype, n.subtype),
                    n.health_status = coalesce(ent.health_status, n.health_status),
                    n.affiliation = coalesce(ent.affiliation, n.affiliation, [])
                """;
        tx.run(cypher, Map.of("entities", entities.stream().map(this::asMap).toList()));
    }

    private void writeRelationships(TransactionContext tx, List<Relationship> rels) {
        if (rels == null || rels.isEmpty()) {
            return;
        }
        String cypher = """
                UNWIND $rels AS rel
                MATCH (a:Entity {name: rel.source})
                MATCH (b:Entity {name: rel.target})
                MERGE (a)-[r:RELATION {type: rel.type}]->(b)
                ON CREATE SET r.description = rel.description, r.strength = rel.strength, r.is_temporary = rel.is_temporary
                ON MATCH SET r.description = coalesce(rel.description, r.description),
                              r.strength = coalesce(rel.strength, r.strength),
                              r.is_temporary = coalesce(rel.is_temporary, r.is_temporary)
                """;
        tx.run(cypher, Map.of("rels", rels.stream().map(this::asMap).toList()));
    }

    private void writeEvents(TransactionContext tx, List<PlotEvent> events) {
        if (events == null || events.isEmpty()) {
            return;
        }
        String cypher = """
                UNWIND $events AS ev
                MERGE (e:PlotEvent {order_id: ev.order_id, chunk_id: ev.chunk_id})
                ON CREATE SET e.summary = ev.summary, e.participants = ev.participants,
                              e.time_of_day = ev.time_of_day, e.day_sequence = ev.day_sequence,
                              e.is_flashback = ev.is_flashback,
                              e.atmosphere = ev.atmosphere,
                              e.key_dialogue = ev.key_dialogue
                ON MATCH SET e.summary = coalesce(e.summary, ev.summary),
                              e.participants = coalesce(ev.participants, e.participants, []),
                              e.time_of_day = coalesce(ev.time_of_day, e.time_of_day),
                              e.day_sequence = coalesce(ev.day_sequence, e.day_sequence),
                              e.is_flashback = coalesce(ev.is_flashback, e.is_flashback),
                              e.atmosphere = coalesce(ev.atmosphere, e.atmosphere),
                              e.key_dialogue = coalesce(ev.key_dialogue, e.key_dialogue)
                """;
        tx.run(cypher, Map.of("events", events.stream().map(this::asMap).toList()));
    }

    private void linkParticipants(TransactionContext tx, String chunkId) {
        if (chunkId == null || chunkId.isBlank()) {
            return;
        }
        String cypher = """
                MATCH (e:PlotEvent {chunk_id: $chunk_id})
                WHERE e.participants IS NOT NULL
                UNWIND e.participants AS name
                MATCH (p:Entity) WHERE p.name = name OR name IN coalesce(p.aliases, [])
                MERGE (p)-[:PARTICIPATED_IN]->(e)
                """;
        tx.run(cypher, Map.of("chunk_id", chunkId));
    }

    private void linkTimeline(TransactionContext tx, String chunkId) {
        if (chunkId == null || chunkId.isBlank()) {
            return;
        }
        String cypher = """
                MATCH (e:PlotEvent {chunk_id: $chunk_id})
                WITH e ORDER BY e.order_id ASC
                WITH collect(e) as events
                FOREACH (i IN range(0, size(events)-2) |
                    FOREACH (current IN [events[i]] |
                        FOREACH (next IN [events[i+1]] |
                            MERGE (current)-[:NEXT_EVENT]->(next)
                        ))
                )
                """;
        tx.run(cypher, Map.of("chunk_id", chunkId));
    }

    private Map<String, Object> asMap(Entity e) {
        return Map.of(
                "name", e.getName(),
                "type", e.getType() != null ? e.getType().name() : Enums.EntityType.Person.name(),
                "subtype", e.getSubtype() != null ? e.getSubtype().name() : Enums.EntitySubtype.Unknown.name(),
                "aliases", e.getAliases(),
                "description", e.getDescription(),
                "health_status", e.getHealthStatus() != null ? e.getHealthStatus().name() : Enums.HealthStatus.Unknown.name(),
                "affiliation", e.getAffiliation()
        );
    }

    private Map<String, Object> asMap(Relationship r) {
        return Map.of(
                "source", r.getSource(),
                "target", r.getTarget(),
                "type", r.getType() != null ? r.getType().name() : Enums.RelationshipType.RELATED_TO.name(),
                "description", r.getDescription(),
                "strength", r.getStrength(),
                "is_temporary", r.getIsTemporary()
        );
    }

    private Map<String, Object> asMap(PlotEvent e) {
        return Map.of(
                "order_id", e.getOrderId(),
                "summary", e.getSummary(),
                "participants", e.getParticipants(),
                "time_of_day", e.getTimeOfDay() != null ? e.getTimeOfDay().name() : Enums.TimeOfDay.Unknown.name(),
                "day_sequence", e.getDaySequence(),
                "is_flashback", e.getIsFlashback(),
                "atmosphere", e.getAtmosphere(),
                "key_dialogue", e.getKeyDialogue(),
                "chunk_id", e.getChunkId()
        );
    }
}
