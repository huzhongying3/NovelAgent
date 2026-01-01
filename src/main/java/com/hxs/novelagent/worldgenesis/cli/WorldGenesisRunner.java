package com.hxs.novelagent.worldgenesis.cli;

import java.nio.file.Path;
import java.util.List;
import java.util.Optional;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.hxs.novelagent.worldgenesis.config.WorldGenesisProperties;
import com.hxs.novelagent.worldgenesis.model.WorldBible;
import com.hxs.novelagent.worldgenesis.service.SeedDataService;
import com.hxs.novelagent.worldgenesis.service.WorldGenesisService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(name = "worldgenesis.cli.enabled", havingValue = "true")
public class WorldGenesisRunner implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(WorldGenesisRunner.class);

    private final WorldGenesisService worldGenesisService;
    private final SeedDataService seedDataService;
    private final WorldGenesisProperties properties;
    private final ObjectMapper objectMapper;

    public WorldGenesisRunner(
            WorldGenesisService worldGenesisService,
            SeedDataService seedDataService,
            WorldGenesisProperties properties,
            ObjectMapper objectMapper
    ) {
        this.worldGenesisService = worldGenesisService;
        this.seedDataService = seedDataService;
        this.properties = properties;
        this.objectMapper = objectMapper;
    }

    @Override
    public void run(ApplicationArguments args) throws Exception {
        if (args.containsOption("help")) {
            printHelp();
            return;
        }

        boolean shouldSeed = args.containsOption("seed") || args.containsOption("seed-file");
        boolean seedOnly = args.containsOption("seed-only");
        if (shouldSeed) {
            Path dataFile = resolveSeedFile(args);
            seedDataService.seedData(dataFile);
            if (seedOnly) {
                return;
            }
        } else if (seedOnly) {
            log.warn("--seed-only 已指定但未提供 --seed 或 --seed-file，跳过。");
        }

        boolean skipGenerate = args.containsOption("skip-generate");
        if (skipGenerate) {
            return;
        }

        Integer sampleSize = parseSampleSize(args);
        String outputPath = parseOutputPath(args);

        WorldBible bible = worldGenesisService.generateSourceBible(sampleSize, outputPath);
        System.out.println(objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(bible));
    }

    private Path resolveSeedFile(ApplicationArguments args) {
        Optional<String> seedFileOpt = firstArg(args, "seed-file");
        if (seedFileOpt.isPresent()) {
            return Path.of(seedFileOpt.get());
        }
        return seedDataService.getDefaultDataFile();
    }

    private Integer parseSampleSize(ApplicationArguments args) {
        Optional<String> sizeOpt = firstArg(args, "sample-size");
        if (sizeOpt.isEmpty()) {
            return null;
        }
        try {
            return Integer.parseInt(sizeOpt.get());
        } catch (NumberFormatException ex) {
            throw new IllegalArgumentException("sample-size 必须为整数: " + sizeOpt.get());
        }
    }

    private String parseOutputPath(ApplicationArguments args) {
        String output = firstArg(args, "output").orElse(properties.getDefaultOutput());
        if (output != null && output.isBlank()) {
            return null;
        }
        return output;
    }

    private Optional<String> firstArg(ApplicationArguments args, String name) {
        List<String> values = args.getOptionValues(name);
        if (values == null || values.isEmpty()) {
            return Optional.empty();
        }
        return Optional.ofNullable(values.get(0));
    }

    private void printHelp() {
        String usage = """
                WorldGenesis CLI（Spring Boot）
                用法:
                  java -jar world-genesis.jar [--sample-size=40] [--output=source_world_bible.json]
                  java -jar world-genesis.jar --seed --seed-file=records-15.json

                主要参数:
                  --sample-size    采样 PlotEvent 数量（默认 WORLD_SAMPLE_SIZE 或 40）
                  --output         生成文件名，传空字符串跳过写文件
                  --seed           使用默认数据文件导入 Neo4j（可配合 --seed-file 覆盖）
                  --seed-file      自定义导入文件路径
                  --seed-only      只导入数据，不触发世界观生成
                  --skip-generate  跳过世界观生成（仅做导入或其他前置操作）
                  --help           显示本帮助
                """;
        System.out.println(usage);
    }
}
