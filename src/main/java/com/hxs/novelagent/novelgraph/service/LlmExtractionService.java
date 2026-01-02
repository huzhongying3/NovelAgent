package com.hxs.novelagent.novelgraph.service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.hxs.novelagent.novelgraph.model.ChunkSummary;
import com.hxs.novelagent.novelgraph.model.Entity;
import com.hxs.novelagent.novelgraph.model.Enums;
import com.hxs.novelagent.novelgraph.model.ExtractionResult;
import com.hxs.novelagent.novelgraph.model.PlotEvent;
import com.hxs.novelagent.novelgraph.model.Relationship;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.openai.OpenAiChatOptions;
import org.springframework.ai.openai.api.OpenAiApi.ChatCompletionRequest.ResponseFormat;
import org.springframework.stereotype.Service;

@Service
public class LlmExtractionService {

    private static final Logger log = LoggerFactory.getLogger(LlmExtractionService.class);

    private static final String SYSTEM_PROMPT = """
            You are a literary analyst extracting an event-centric knowledge graph from a Chinese novel.
            Data governance: keys must be snake_case English; values in Chinese (except enums/IDs); arrays stay arrays;
            types/subtypes/relationship types must use allowed enums; relationships include strength (1-10) and is_temporary;
            infer time_of_day and maintain temporal consistency; always list participants explicitly on events;
            also extract screenwriter dimensions atmosphere and key_dialogue when present.
            """;

    private static final String SCHEMA_GUIDE = """
            Use EXACT field names and JSON structure:
            {
              "chunk_summary": {"content": "...", "main_characters": ["张三","李四"]},
              "entities": [
                {"name": "张三", "type": "Person", "subtype": "MainCharacter", "aliases": ["三哥"], "health_status": "Healthy", "affiliation": ["None"], "description": "..."}
              ],
              "relationships": [
                {"source": "张三", "target": "李四", "type": "RELATED_TO", "description": "父女关系", "strength": 10, "is_temporary": false}
              ],
              "events": [
                {"order_id": 1, "summary": "张三在深夜逃亡", "participants": ["张三", "白马"], "time_of_day": "Night", "day_sequence": 1, "is_flashback": false, "atmosphere": "Tense", "key_dialogue": "我不能停下！"}
              ],
              "next_summary": "1-2 sentences for the next chunk",
              "active_characters": ["张三", "李四"]
            }
            Allowed enums: entity.type ∈ {Person, Location, Organization, Item, Concept}; entity.subtype ∈ {MainCharacter, SupportingCharacter, Antagonist, Mob, Weapon, Medicine, Book, Tool, Treasure, City, Wilderness, Building, Room, Sect, Government, Family, Unknown}; health_status ∈ {Healthy, Injured, Critical, Deceased, Poisoned, Unknown}; relationship.type ∈ {ATTACKED, PROTECTED, KILLED, SAVED, KNOWS, TALKED_TO, RELATED_TO, LOVES, HATES, MASTER_OF, STUDENT_OF, MEMBER_OF, OWNS, USED, LOST, LOCATED_AT, TRAVELLED_TO}; events.time_of_day ∈ {Morning, Noon, Afternoon, Evening, Night, Unknown}.
            Extract 3-8 key events per chunk with ascending order_id; participants must be an array.
            """;

    private final ChatModel chatModel;
    private final ObjectMapper objectMapper;

    public LlmExtractionService(ChatModel chatModel, ObjectMapper objectMapper) {
        this.chatModel = chatModel;
        this.objectMapper = objectMapper;
    }

    public ExtractionResult extract(String currentText, String previousSummary, String context, List<String> registry, List<String> active, String chunkId, String model, double temperature) {
        String user = buildUserPrompt(currentText, previousSummary, context, registry, active);
        Prompt prompt = new Prompt(
                List.of(new SystemMessage(SYSTEM_PROMPT), new UserMessage(user)),
                OpenAiChatOptions.builder()
                        .withModel(model)
                        .withTemperature((float) temperature)
                        .withResponseFormat(new ResponseFormat(ResponseFormat.Type.JSON_OBJECT))
                        .build()
        );
        var resp = chatModel.call(prompt);
        String content = resp.getResult().getOutput().getContent();
        if (content == null || content.isBlank()) {
            throw new IllegalStateException("LLM 返回空内容");
        }
        Map<String, Object> data = parseJson(content);
        return mapToResult(data, chunkId);
    }

    private String buildUserPrompt(String currentText, String previousSummary, String context, List<String> registry, List<String> active) {
        String kb = context == null ? "" : context;
        String known = registry == null || registry.isEmpty() ? "无" : String.join(",", registry);
        String act = active == null || active.isEmpty() ? "无" : String.join(",", active);
        return """
                Normalization rule: I will provide an [Existing Knowledge Base]. If a name or alias in the text matches one in the KB, use the KB's standard name as the entity name, and append any new epithets or surface forms to aliases. Only merge when role/description roughly aligns; otherwise create a new entity.
                Redundancy: events must list all participants by standard names. Temporal consistency: if time_of_day transitions Night->Morning and not flashback, increment day_sequence; if flashback, keep day_sequence and set is_flashback=true.
                Screenwriter dimensions: extract atmosphere (Tense/Humorous/Melancholic/Romantic/Action-packed/etc.) and one key_dialogue line if present.

                [Existing Knowledge Base]
                %s
                Previous story summary: %s
                Known core characters: %s
                Active characters: %s

                %s

                Current text:
                %s
                Respond with JSON matching the schema above.
                """.formatted(kb, previousSummary, known, act, SCHEMA_GUIDE, currentText);
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> parseJson(String content) {
        try {
            return objectMapper.readValue(content, Map.class);
        } catch (Exception e) {
            String trimmed = content.trim();
            int start = trimmed.indexOf('{');
            int end = trimmed.lastIndexOf('}');
            if (start >= 0 && end > start) {
                try {
                    return objectMapper.readValue(trimmed.substring(start, end + 1), Map.class);
                } catch (Exception ignored) {
                }
            }
            throw new IllegalStateException("解析 LLM JSON 失败: " + content, e);
        }
    }

    @SuppressWarnings("unchecked")
    private ExtractionResult mapToResult(Map<String, Object> data, String chunkId) {
        ExtractionResult result = new ExtractionResult();
        Map<String, Object> chunkMap = (Map<String, Object>) data.getOrDefault("chunk_summary", Map.of());
        ChunkSummary cs = new ChunkSummary();
        cs.setContent(asString(chunkMap.get("content")));
        cs.setMainCharacters(toStringList(chunkMap.get("main_characters")));
        result.setChunkSummary(cs);
        result.setEntities(parseEntities((List<Object>) data.getOrDefault("entities", List.of())));
        result.setRelationships(parseRelationships((List<Object>) data.getOrDefault("relationships", List.of())));
        result.setEvents(parseEvents((List<Object>) data.getOrDefault("events", List.of()), chunkId));
        result.setNextSummary(asString(data.getOrDefault("next_summary", "")));
        result.setActiveCharacters(toStringList(data.get("active_characters")));
        return result;
    }

    private List<Entity> parseEntities(List<Object> list) {
        List<Entity> res = new ArrayList<>();
        for (Object o : list) {
            if (o instanceof Map<?, ?> map) {
                Entity e = new Entity();
                e.setName(asString(map.get("name")));
                e.setType(safeEnum(Enums.EntityType.class, map.get("type"), Enums.EntityType.Person));
                e.setSubtype(safeEnum(Enums.EntitySubtype.class, map.get("subtype"), Enums.EntitySubtype.Unknown));
                e.setAliases(toStringList(map.get("aliases")));
                e.setHealthStatus(safeEnum(Enums.HealthStatus.class, map.get("health_status"), Enums.HealthStatus.Unknown));
                e.setAffiliation(toStringList(map.get("affiliation")));
                e.setDescription(asString(map.get("description")));
                if (e.getName() != null && !e.getName().isBlank()) {
                    res.add(e);
                }
            }
        }
        return res;
    }

    private List<Relationship> parseRelationships(List<Object> list) {
        List<Relationship> res = new ArrayList<>();
        for (Object o : list) {
            if (o instanceof Map<?, ?> map) {
                Relationship r = new Relationship();
                r.setSource(asString(map.get("source")));
                r.setTarget(asString(map.get("target")));
                r.setType(safeEnum(Enums.RelationshipType.class, map.get("type"), Enums.RelationshipType.RELATED_TO));
                r.setDescription(asString(map.get("description")));
                Object strength = map.get("strength");
                r.setStrength(strength instanceof Number ? ((Number) strength).intValue() : null);
                Object tmp = map.get("is_temporary");
                r.setIsTemporary(tmp instanceof Boolean ? (Boolean) tmp : null);
                if (r.getSource() != null && r.getTarget() != null && !r.getSource().isBlank() && !r.getTarget().isBlank()) {
                    res.add(r);
                }
            }
        }
        return res;
    }

    private List<PlotEvent> parseEvents(List<Object> list, String chunkId) {
        List<PlotEvent> res = new ArrayList<>();
        int idx = 1;
        for (Object o : list) {
            if (o instanceof Map<?, ?> map) {
                PlotEvent ev = new PlotEvent();
                Object order = map.get("order_id");
                ev.setOrderId(order instanceof Number ? ((Number) order).intValue() : idx);
                ev.setSummary(asString(map.get("summary")));
                ev.setParticipants(toStringList(map.get("participants")));
                ev.setTimeOfDay(safeEnum(Enums.TimeOfDay.class, map.get("time_of_day"), Enums.TimeOfDay.Unknown));
                ev.setDaySequence(asInt(map.get("day_sequence"), 1));
                Object fb = map.get("is_flashback");
                ev.setIsFlashback(fb instanceof Boolean ? (Boolean) fb : false);
                ev.setAtmosphere(asString(map.get("atmosphere")));
                ev.setKeyDialogue(asString(map.get("key_dialogue")));
                ev.setTimestampRelative(asInt(map.get("timestamp_relative"), null));
                ev.setChunkId(chunkId);
                res.add(ev);
                idx++;
            }
        }
        return res;
    }

    private List<String> toStringList(Object obj) {
        List<String> res = new ArrayList<>();
        if (obj instanceof List<?> list) {
            for (Object o : list) {
                if (o != null && !o.toString().isBlank()) {
                    res.add(o.toString());
                }
            }
        } else if (obj instanceof String s) {
            if (!s.isBlank()) {
                res.add(s);
            }
        }
        return res;
    }

    private String asString(Object obj) {
        return obj == null ? "" : obj.toString();
    }

    private Integer asInt(Object obj, Integer fallback) {
        if (obj instanceof Number n) {
            return n.intValue();
        }
        try {
            return obj != null ? Integer.parseInt(obj.toString()) : fallback;
        } catch (Exception e) {
            return fallback;
        }
    }

    private <E extends Enum<E>> E safeEnum(Class<E> type, Object val, E fallback) {
        if (val == null) {
            return fallback;
        }
        try {
            return Enum.valueOf(type, val.toString());
        } catch (Exception e) {
            return fallback;
        }
    }
}
