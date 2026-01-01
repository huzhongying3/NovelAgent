package com.hxs.novelagent.worldgenesis.service;

import java.io.IOException;
import java.util.*;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.MissingNode;
import com.hxs.novelagent.worldgenesis.model.WorldBible;
import org.springframework.stereotype.Component;

@Component
public class WorldBibleNormalizer {

    private static final List<String> POWER_KEYS = Arrays.asList("power_system", "power", "powerSystem");
    private static final List<String> POWER_LIMIT_KEYS = Arrays.asList(
            "power_limitations", "power_limits", "limits", "costs", "power_constraints"
    );
    private static final List<String> SOCIAL_KEYS = Arrays.asList(
            "social_hierarchy", "social_hierarchy_and_economy", "hierarchy"
    );
    private static final List<String> CONFLICT_KEYS = Arrays.asList(
            "world_conflict_source", "conflict", "core_conflict", "conflict_source"
    );
    private static final List<String> ECONOMIC_KEYS = Arrays.asList(
            "economic_rules", "economic_system", "economy"
    );
    private static final List<String> FACTION_KEYS = Arrays.asList(
            "major_factions", "core_factions", "factions"
    );
    private static final List<String> GEO_KEYS = Arrays.asList(
            "geographical_scope", "geography", "locations"
    );
    private static final List<String> TABOO_KEYS = Arrays.asList(
            "cultural_taboos", "cultural_and_legal_norms", "taboos"
    );
    private static final List<String> TERMINOLOGY_KEYS = Arrays.asList(
            "specific_terminology", "terminology", "proper_nouns", "unique_terms", "keywords"
    );

    private final ObjectMapper objectMapper;

    public WorldBibleNormalizer(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public WorldBible normalize(String rawJson) {
        JsonNode root;
        try {
            root = objectMapper.readTree(rawJson);
        } catch (IOException e) {
            throw new IllegalArgumentException("LLM 输出无法解析为 JSON: " + rawJson, e);
        }

        JsonNode candidate = root;
        if (root.isObject() && root.has("world_rules")) {
            candidate = root.get("world_rules");
        }
        if (candidate == null || candidate.isMissingNode() || candidate.isNull()) {
            throw new IllegalArgumentException("LLM 输出缺少 world_rules 字段: " + rawJson);
        }

        JsonNode socialNode = pick(candidate, SOCIAL_KEYS);
        JsonNode economicNode = pick(candidate, ECONOMIC_KEYS);
        if ((economicNode == null || economicNode.isMissingNode() || economicNode.isNull())
                && socialNode != null
                && socialNode.isObject()) {
            economicNode = pick(socialNode, Arrays.asList("economic_system", "economy"));
        }

        List<String> majorFactions = toList(pick(candidate, FACTION_KEYS));
        List<String> terminology = toList(pick(candidate, TERMINOLOGY_KEYS));

        if (majorFactions.isEmpty()) {
            majorFactions = List.of("（LLM 未提供核心势力，需人工补充）");
        }
        if (terminology.isEmpty()) {
            terminology = List.of("（LLM 未提取专有名词，需人工补充）");
        }

        return new WorldBible(
                ensureText(pick(candidate, POWER_KEYS), "（LLM 未提供力量体系，需人工补充）"),
                ensureText(pick(candidate, POWER_LIMIT_KEYS), "（LLM 未提供力量代价/限制，需人工补充）"),
                ensureText(pick(candidate, GEO_KEYS), "（LLM 未提供地理范围，需人工补充）"),
                ensureText(socialNode, "（LLM 未提供社会阶层，需人工补充）"),
                ensureText(pick(candidate, CONFLICT_KEYS), "（LLM 未提供核心矛盾，需人工补充）"),
                majorFactions,
                ensureText(economicNode, "（LLM 未提供经济规则，需人工补充）"),
                ensureText(pick(candidate, TABOO_KEYS), "（LLM 未提供文化禁忌，需人工补充）"),
                terminology
        );
    }

    private JsonNode pick(JsonNode node, List<String> keys) {
        if (node == null || node.isMissingNode() || node.isNull()) {
            return MissingNode.getInstance();
        }
        for (String key : keys) {
            if (node.has(key)) {
                return node.get(key);
            }
        }
        return MissingNode.getInstance();
    }

    private String ensureText(JsonNode node, String fallback) {
        String text = asText(node);
        return text.isBlank() ? fallback : text;
    }

    private String asText(JsonNode node) {
        if (node == null || node.isMissingNode() || node.isNull()) {
            return "";
        }
        if (node.isArray()) {
            List<String> parts = new ArrayList<>();
            node.forEach(item -> {
                String text = asText(item);
                if (!text.isBlank()) {
                    parts.add(text);
                }
            });
            return String.join("; ", parts);
        }
        if (node.isObject()) {
            List<String> parts = new ArrayList<>();
            for (Map.Entry<String, JsonNode> entry : iterable(node.fields())) {
                String text = asText(entry.getValue());
                if (!text.isBlank()) {
                    parts.add(entry.getKey() + ": " + text);
                }
            }
            return String.join("; ", parts);
        }
        return node.asText("").trim();
    }

    private List<String> toList(JsonNode node) {
        if (node == null || node.isMissingNode() || node.isNull()) {
            return new ArrayList<>();
        }
        if (node.isArray()) {
            List<String> values = new ArrayList<>();
            node.forEach(item -> {
                String text = asText(item);
                if (!text.isBlank()) {
                    values.add(text);
                }
            });
            return values;
        }
        if (node.isObject()) {
            List<String> values = new ArrayList<>();
            for (Map.Entry<String, JsonNode> entry : iterable(node.fields())) {
                String text = asText(entry.getValue());
                if (!text.isBlank()) {
                    values.add(entry.getKey() + ": " + text);
                }
            }
            return values;
        }
        String text = asText(node);
        return text.isBlank() ? new ArrayList<>() : new ArrayList<>(List.of(text));
    }

    private static <T> Iterable<T> iterable(Iterator<T> iterator) {
        return () -> iterator;
    }
}
