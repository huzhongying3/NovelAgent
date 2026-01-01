package com.hxs.novelagent.worldgenesis.api;

import java.nio.file.Path;

import com.hxs.novelagent.worldgenesis.config.WorldGenesisProperties;
import com.hxs.novelagent.worldgenesis.model.WorldBible;
import com.hxs.novelagent.worldgenesis.service.WorldGenesisService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/worldgenesis")
public class WorldGenesisController {

    private final WorldGenesisService worldGenesisService;
    private final WorldGenesisProperties properties;

    public WorldGenesisController(
            WorldGenesisService worldGenesisService,
            WorldGenesisProperties properties
    ) {
        this.worldGenesisService = worldGenesisService;
        this.properties = properties;
    }

    @PostMapping("/generate")
    public ResponseEntity<WorldBible> generate(@RequestBody(required = false) GenerateRequest request) {
        Integer sampleSize = request != null ? request.sampleSize() : null;
        String output = request != null ? request.output() : properties.getDefaultOutput();
        if (output != null && output.isBlank()) {
            output = null;
        }
        WorldBible bible = worldGenesisService.generateSourceBible(sampleSize, output);
        return ResponseEntity.ok(bible);
    }

    public record GenerateRequest(Integer sampleSize, String output) {
    }

}
