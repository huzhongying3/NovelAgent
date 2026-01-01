package com.hxs.novelagent.worldgenesis.service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.hxs.novelagent.worldgenesis.model.WorldBible;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class WorldGenesisService {

    private static final Logger log = LoggerFactory.getLogger(WorldGenesisService.class);

    private final GraphService graphService;
    private final LlmService llmService;
    private final ObjectMapper objectMapper;

    public WorldGenesisService(GraphService graphService, LlmService llmService, ObjectMapper objectMapper) {
        this.graphService = graphService;
        this.llmService = llmService;
        this.objectMapper = objectMapper;
    }

    public WorldBible generateSourceBible(Integer sampleSize, String outputPath) {
        String contextText = graphService.fetchWorldContext(sampleSize);
        WorldBible bible = llmService.analyzeWorldRules(contextText);

        if (outputPath != null && !outputPath.isBlank()) {
            Path path = Path.of(outputPath);
            try {
                if (path.getParent() != null) {
                    Files.createDirectories(path.getParent());
                }
                objectMapper.writerWithDefaultPrettyPrinter().writeValue(path.toFile(), bible);
                log.info("WorldGenesis: {} 生成完成。", path);
            } catch (IOException e) {
                throw new IllegalStateException("写入文件失败: " + outputPath, e);
            }
        }

        return bible;
    }
}
