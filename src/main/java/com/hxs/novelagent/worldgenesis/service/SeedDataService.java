package com.hxs.novelagent.worldgenesis.service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Consumer;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.hxs.novelagent.worldgenesis.config.WorldGenesisProperties;
import org.neo4j.driver.Driver;
import org.neo4j.driver.Session;
import org.neo4j.driver.TransactionContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class SeedDataService {

    private static final Logger log = LoggerFactory.getLogger(SeedDataService.class);

    private static final String DEFAULT_SUBTYPE = "SupportingCharacter";

    private final Driver driver;
    private final ObjectMapper objectMapper;
    private final WorldGenesisProperties properties;

    public SeedDataService(Driver driver, ObjectMapper objectMapper, WorldGenesisProperties properties) {
        this.driver = driver;
        this.objectMapper = objectMapper;
        this.properties = properties;
    }

    public SeedResult seedData(Path filePath) {
        Path resolved = filePath;
        if (!filePath.isAbsolute()) {
            resolved = Path.of("").toAbsolutePath().resolve(filePath).normalize();
        }
        if (!Files.exists(resolved)) {
            throw new IllegalArgumentException("找不到数据文件: " + resolved);
        }
        DataSet data = loadData(resolved);

        try (Session session = driver.session()) {
            if (!data.entities().isEmpty()) {
                session.executeWriteWithoutResult(tx -> seedEntities(tx, data.entities()));
            }
            session.executeWriteWithoutResult(tx -> seedPlotEvents(tx, data.plotEvents()));
        }

        log.info("导入完成：Entity {} 条，PlotEvent {} 条。", data.entities().size(), data.plotEvents().size());
        return new SeedResult(resolved, data.entities().size(), data.plotEvents().size());
    }

    public Path getDefaultDataFile() {
        return Path.of(properties.getDefaultDataFile());
    }

    private DataSet loadData(Path filePath) {
        JsonNode root;
        try {
            root = objectMapper.readTree(filePath.toFile());
        } catch (IOException e) {
            throw new IllegalStateException("读取数据文件失败: " + filePath, e);
        }

        List<Map<String, Object>> entities = new ArrayList<>();
        List<Map<String, Object>> plotEvents = new ArrayList<>();

        if (root.isObject()) {
            JsonNode entList = root.get("entities") != null ? root.get("entities") : root.get("Entities");
            JsonNode evtList = root.get("plot_events") != null
                    ? root.get("plot_events")
                    : (root.get("PlotEvents") != null ? root.get("PlotEvents") : root.get("events"));
            if ((entList == null || entList.isEmpty()) && (evtList == null || evtList.isEmpty()) && root.has("records")) {
                evtList = root.get("records");
            }

            handleCollection(entList, node -> addEntity(node, entities));
            handleCollection(evtList, node -> addEvent(node, plotEvents));
        } else if (root.isArray()) {
            handleCollection(root, node -> addEvent(node, plotEvents));
        }

        if (plotEvents.isEmpty()) {
            walk(root, node -> addEvent(node, plotEvents), node -> addEntity(node, entities));
        }

        if (plotEvents.isEmpty()) {
            throw new IllegalStateException("未解析到任何 PlotEvent，请检查 JSON 结构或键名。");
        }
        if (entities.isEmpty()) {
            log.warn("警告：未解析到 Entity，将仅写入 PlotEvent。");
        }

        return new DataSet(entities, plotEvents);
    }

    private void handleCollection(JsonNode node, Consumer<JsonNode> consumer) {
        if (node == null || node.isMissingNode() || node.isNull()) {
            return;
        }
        if (node.isArray()) {
            node.forEach(consumer);
        } else {
            consumer.accept(node);
        }
    }

    private void walk(JsonNode node, Consumer<JsonNode> eventConsumer, Consumer<JsonNode> entityConsumer) {
        if (node == null || node.isMissingNode() || node.isNull()) {
            return;
        }
        if (node.isArray()) {
            node.forEach(child -> walk(child, eventConsumer, entityConsumer));
        } else if (node.isObject()) {
            eventConsumer.accept(node);
            entityConsumer.accept(node);
            for (Iterator<JsonNode> it = node.elements(); it.hasNext(); ) {
                walk(it.next(), eventConsumer, entityConsumer);
            }
        }
    }

    private void addEvent(JsonNode node, List<Map<String, Object>> plotEvents) {
        JsonNode target = node;
        if (node.isObject() && node.has("n")) {
            target = node.get("n");
        }

        List<String> labels = extractLabels(target);
        JsonNode props = extractProps(target);
        if (!labels.contains("PlotEvent") && props.path("summary").asText("").isBlank()) {
            return;
        }

        plotEvents.add(Map.of(
                "summary", props.path("summary").asText(""),
                "day_sequence", extractNumberOrText(props.get("day_sequence")),
                "atmosphere", props.path("atmosphere").asText(""),
                "key_dialogue", props.path("key_dialogue").asText(""),
                "participants", toList(props.get("participants")),
                "chunk_id", props.has("chunk_id") ? props.get("chunk_id").asText(null) : null
        ));
    }

    private void addEntity(JsonNode node, List<Map<String, Object>> entities) {
        JsonNode target = node;
        if (node.isObject() && node.has("n")) {
            target = node.get("n");
        }

        List<String> labels = extractLabels(target);
        JsonNode props = extractProps(target);
        boolean looksLikeEntity = props.has("name") && !props.path("name").asText("").isBlank()
                && props.has("description") && !props.path("description").asText("").isBlank();
        if (!labels.contains("Entity") && !looksLikeEntity) {
            return;
        }

        entities.add(Map.of(
                "name", props.path("name").asText(""),
                "description", props.path("description").asText(""),
                "subtype", props.path("subtype").asText(DEFAULT_SUBTYPE),
                "aliases", toList(props.get("aliases")),
                "affiliation", toList(props.get("affiliation"))
        ));
    }

    private Object extractNumberOrText(JsonNode node) {
        if (node == null || node.isMissingNode() || node.isNull()) {
            return null;
        }
        if (node.isNumber()) {
            return node.numberValue();
        }
        String text = node.asText("");
        return text.isBlank() ? null : text;
    }

    private JsonNode extractProps(JsonNode node) {
        if (node == null || node.isMissingNode() || node.isNull()) {
            return node;
        }
        if (node.has("properties") && node.get("properties").isObject()) {
            return node.get("properties");
        }
        return node;
    }

    private List<String> extractLabels(JsonNode node) {
        if (node != null && node.has("labels") && node.get("labels").isArray()) {
            List<String> labels = new ArrayList<>();
            node.get("labels").forEach(label -> labels.add(label.asText("")));
            return labels;
        }
        return List.of();
    }

    private List<String> toList(JsonNode node) {
        if (node == null || node.isMissingNode() || node.isNull()) {
            return new ArrayList<>();
        }
        if (node.isArray()) {
            List<String> values = new ArrayList<>();
            node.forEach(n -> {
                String text = n.asText("");
                if (!text.isBlank()) {
                    values.add(text);
                }
            });
            return values;
        }
        String text = node.asText("");
        if (text.isBlank()) {
            return new ArrayList<>();
        }
        return new ArrayList<>(List.of(text));
    }

    private void seedEntities(TransactionContext tx, List<Map<String, Object>> entities) {
        String query = """
                MERGE (e:Entity {name: $name})
                SET e.description = $description,
                    e.subtype = $subtype,
                    e.aliases = $aliases,
                    e.affiliation = $affiliation
                """;
        for (Map<String, Object> ent : entities) {
            tx.run(query, ent);
        }
    }

    private void seedPlotEvents(TransactionContext tx, List<Map<String, Object>> events) {
        String query = """
                MERGE (p:PlotEvent {summary: $summary, day_sequence: $day_sequence})
                SET p.atmosphere = $atmosphere,
                    p.key_dialogue = $key_dialogue,
                    p.participants = $participants,
                    p.chunk_id = $chunk_id
                """;
        for (Map<String, Object> ev : events) {
            tx.run(query, ev);
        }
    }

    private record DataSet(List<Map<String, Object>> entities, List<Map<String, Object>> plotEvents) {
        private DataSet {
            Objects.requireNonNull(entities);
            Objects.requireNonNull(plotEvents);
        }
    }

    public record SeedResult(Path dataFile, int entities, int plotEvents) {
    }
}
