package com.hxs.novelagent.worldgenesis.service;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import com.hxs.novelagent.worldgenesis.config.WorldGenesisProperties;
import org.neo4j.driver.Driver;
import org.neo4j.driver.Record;
import org.neo4j.driver.Session;
import org.neo4j.driver.Value;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service("worldGenesisGraphService")
public class GraphService {

    private static final Logger log = LoggerFactory.getLogger(GraphService.class);

    private static final String MAIN_CHARS_QUERY = """
            MATCH (n:Entity {subtype: 'MainCharacter'})
            RETURN collect(n.name + ': ' + n.description) AS main_chars
            """;

    private static final String EVENTS_QUERY = """
            MATCH (e:PlotEvent)
            WHERE e.summary IS NOT NULL
            RETURN e.summary AS summary,
                   e.atmosphere AS atmosphere,
                   e.key_dialogue AS key_dialogue,
                   e.day_sequence AS day_sequence
            ORDER BY e.day_sequence ASC
            LIMIT $fetch_limit
            """;

    private final Driver driver;
    private final WorldGenesisProperties properties;

    public GraphService(Driver driver, WorldGenesisProperties properties) {
        this.driver = driver;
        this.properties = properties;
    }

    public String fetchWorldContext(Integer sampleSizeOverride) {
        int size = (sampleSizeOverride != null && sampleSizeOverride > 0)
                ? sampleSizeOverride
                : properties.getSampleSize();
        int conflictQuota = Math.max(1, size / 2);
        int timelineQuota = Math.max(1, size - conflictQuota);

        List<String> mainChars;
        List<Map<String, Object>> events;

        try (Session session = driver.session()) {
            Record mainCharsRecord = session.run(MAIN_CHARS_QUERY).single();
            mainChars = ensureNonEmpty(
                    mainCharsRecord.get("main_chars").asList(Value::asString),
                    "MainCharacter 节点"
            );

            List<Record> eventRecords = session.run(
                    EVENTS_QUERY,
                    Map.of("fetch_limit", Math.max(size * 4, size))
            ).list();

            events = ensureNonEmpty(eventRecords.stream()
                            .map(GraphService::mapEvent)
                            .collect(Collectors.toList()),
                    "PlotEvent 节点");
        }

        List<Map<String, Object>> conflictEvents = new ArrayList<>();
        Set<String> conflictKeys = new HashSet<>();
        for (Map<String, Object> ev : events) {
            if (conflictEvents.size() >= conflictQuota) {
                break;
            }
            if (containsConflict(ev.getOrDefault("summary", "").toString())) {
                conflictEvents.add(ev);
                conflictKeys.add(ev.get("summary") + "#" + ev.get("day_sequence"));
            }
        }

        List<Map<String, Object>> remainingEvents = events.stream()
                .filter(ev -> !conflictKeys.contains(ev.get("summary") + "#" + ev.get("day_sequence")))
                .collect(Collectors.toList());

        List<Map<String, Object>> timelineEvents = evenSample(remainingEvents, timelineQuota);

        String context = formatContext(mainChars, conflictEvents, timelineEvents, properties.getMaxContextChars());
        log.debug("生成上下文长度 {} 字符", context.length());
        return context;
    }

    private static Map<String, Object> mapEvent(Record record) {
        return Map.of(
                "summary", safeGet(record, "summary"),
                "atmosphere", safeGet(record, "atmosphere"),
                "key_dialogue", safeGet(record, "key_dialogue"),
                "day_sequence", record.get("day_sequence").isNull() ? null : record.get("day_sequence").asObject()
        );
    }

    private static String safeGet(Record record, String key) {
        Value value = record.get(key);
        return value == null || value.isNull() ? "" : value.asString();
    }

    private boolean containsConflict(String summary) {
        return properties.getConflictKeywords().stream().anyMatch(summary::contains);
    }

    private static List<Map<String, Object>> evenSample(List<Map<String, Object>> events, int quota) {
        if (quota <= 0) {
            return List.of();
        }
        if (events.size() <= quota) {
            return events;
        }
        double step = (double) events.size() / quota;
        List<Map<String, Object>> picks = new ArrayList<>();
        for (int i = 0; i < quota; i++) {
            int idx = (int) (i * step);
            if (idx >= events.size()) {
                idx = events.size() - 1;
            }
            picks.add(events.get(idx));
        }

        Set<String> seen = new HashSet<>();
        List<Map<String, Object>> unique = new ArrayList<>();
        for (Map<String, Object> ev : picks) {
            String key = ev.get("summary") + "#" + ev.get("day_sequence");
            if (seen.add(key)) {
                unique.add(ev);
            }
        }
        return unique;
    }

    private static String formatContext(
            List<String> mainChars,
            List<Map<String, Object>> conflictEvents,
            List<Map<String, Object>> timelineEvents,
            int maxChars
    ) {
        List<String> lines = new ArrayList<>();
        lines.add("## 主要角色");
        mainChars.forEach(c -> lines.add("- " + c));
        lines.add("");

        lines.add("## 高张力事件（冲突采样）");
        if (conflictEvents.isEmpty()) {
            lines.add("- （未采样到含冲突关键词的事件）");
        } else {
            for (int i = 0; i < conflictEvents.size(); i++) {
                Map<String, Object> ev = conflictEvents.get(i);
                String summary = ev.getOrDefault("summary", "").toString().trim();
                Object daySeq = ev.getOrDefault("day_sequence", "N/A");
                if (summary.isEmpty()) {
                    continue;
                }
                lines.add("%d. Day %s: %s".formatted(i + 1, daySeq, summary));
                Object atmosphere = ev.get("atmosphere");
                if (atmosphere != null && !atmosphere.toString().isBlank()) {
                    lines.add("   - 氛围: " + atmosphere);
                }
                Object dialogue = ev.get("key_dialogue");
                if (dialogue != null && !dialogue.toString().isBlank()) {
                    lines.add("   - 关键台词: " + dialogue);
                }
                if (String.join("\n", lines).length() > maxChars) {
                    lines.add("...（上下文截断以控制 Token）");
                    break;
                }
            }
        }

        lines.add("");
        lines.add("## 时序事件采样（覆盖开头/中段/结尾）");
        for (int i = 0; i < timelineEvents.size(); i++) {
            Map<String, Object> ev = timelineEvents.get(i);
            String summary = ev.getOrDefault("summary", "").toString().trim();
            Object daySeq = ev.getOrDefault("day_sequence", "N/A");
            if (summary.isEmpty()) {
                continue;
            }
            lines.add("%d. Day %s: %s".formatted(i + 1, daySeq, summary));
            Object atmosphere = ev.get("atmosphere");
            if (atmosphere != null && !atmosphere.toString().isBlank()) {
                lines.add("   - 氛围: " + atmosphere);
            }
            Object dialogue = ev.get("key_dialogue");
            if (dialogue != null && !dialogue.toString().isBlank()) {
                lines.add("   - 关键台词: " + dialogue);
            }
            if (String.join("\n", lines).length() > maxChars) {
                lines.add("...（上下文截断以控制 Token）");
                break;
            }
        }

        String context = String.join("\n", lines).trim();
        if (context.isEmpty()) {
            throw new IllegalStateException("上下文格式化结果为空，请检查 Neo4j 数据。");
        }
        return context;
    }

    private static <T> List<T> ensureNonEmpty(List<T> items, String kind) {
        if (items == null || items.isEmpty()) {
            throw new IllegalStateException("Neo4j 未找到 " + kind + "，请先写入基础数据后重试。");
        }
        return items;
    }
}
