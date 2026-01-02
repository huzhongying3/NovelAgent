package com.hxs.novelagent.novelgraph.service;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import com.hxs.novelagent.novelgraph.config.NovelGraphProperties;
import com.hxs.novelagent.novelgraph.service.TextIngestionService.TextChunk;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

@Service
public class OrchestratorService {

    private static final Logger log = LoggerFactory.getLogger(OrchestratorService.class);

    private final NovelGraphProperties props;
    private final TextIngestionService ingestionService;
    private final ProgressManager progressManager;
    private final GraphService graphService;
    private final LlmExtractionService llmExtractionService;

    public OrchestratorService(
            NovelGraphProperties props,
            @Qualifier("novelGraphGraphService") GraphService graphService,
            LlmExtractionService llmExtractionService,
            ProgressManager progressManager
    ) {
        this.props = props;
        this.graphService = graphService;
        this.llmExtractionService = llmExtractionService;
        this.ingestionService = new TextIngestionService(props.getChunkSize(), props.getChunkOverlap());
        this.progressManager = progressManager;
    }

    public RunResult run(Path novelPath, boolean reset, String modelOverride, Double temperatureOverride) {
        if (reset || props.isResetBeforeRun()) {
            try {
                graphService.clear();
                progressManager.reset();
            } catch (Exception e) {
                log.warn("清理 Neo4j 或进度失败，继续执行: {}", e.getMessage());
            }
        }
        graphService.initIndexes();
        List<TextChunk> chunks = ingestionService.ingest(novelPath);
        log.info("Loaded {} chunks from {}", chunks.size(), novelPath);

        String previousSummary = "";
        List<String> active = new ArrayList<>();
        int processed = 0;
        int skipped = 0;
        int index = 0;

        String model = modelOverride != null && !modelOverride.isBlank() ? modelOverride : props.getModel();
        double temperature = temperatureOverride != null ? temperatureOverride : props.getTemperature();

        for (TextChunk chunk : chunks) {
            index++;
            if (progressManager.isProcessed(chunk.chunkId())) {
                log.info("跳过已处理 chunk {} ({}/{})", chunk.chunkId(), index, chunks.size());
                skipped++;
                continue;
            }
            log.info("开始处理 chunk {} ({}/{})", chunk.chunkId(), index, chunks.size());
            long start = System.currentTimeMillis();
            try {
                List<Map<String, Object>> context = graphService.fetchContextCandidates(chunk.text(), 20);
                String contextStr = formatContext(context);
                var result = llmExtractionService.extract(
                        chunk.text(),
                        previousSummary,
                        contextStr,
                        active,
                        active,
                        chunk.chunkId(),
                        model,
                        temperature
                );
                graphService.save(result);
                progressManager.mark(chunk.chunkId());
                processed++;
                long cost = System.currentTimeMillis() - start;
                log.info("完成 chunk {} ({}/{})，耗时 {}ms，累计 processed={}, skipped={}",
                        chunk.chunkId(), index, chunks.size(), cost, processed, skipped);
                previousSummary = result.getNextSummary() != null ? result.getNextSummary() : previousSummary;
                if (result.getActiveCharacters() != null && !result.getActiveCharacters().isEmpty()) {
                    active = result.getActiveCharacters();
                }
            } catch (Exception e) {
                log.error("处理 chunk {} 失败: {}", chunk.chunkId(), e.getMessage());
                break;
            }
        }
        return new RunResult(processed, skipped);
    }

    private String formatContext(List<Map<String, Object>> candidates) {
        StringBuilder sb = new StringBuilder();
        for (Map<String, Object> c : candidates) {
            sb.append(c.getOrDefault("name", ""));
            List<?> aliases = (List<?>) c.getOrDefault("aliases", List.of());
            if (aliases != null && !aliases.isEmpty()) {
                sb.append(" (别名: ").append(String.join(",", aliases.stream().map(Object::toString).toList())).append(")");
            }
            String desc = c.getOrDefault("desc", "").toString();
            if (!desc.isBlank()) {
                sb.append(" - ").append(desc);
            }
            sb.append("\n");
        }
        return sb.toString();
    }

    public record RunResult(int processedChunks, int skippedChunks) {
    }
}
