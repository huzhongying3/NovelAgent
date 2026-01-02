package com.hxs.novelagent.novelgraph.api;

import java.nio.file.Path;

import com.hxs.novelagent.novelgraph.service.OrchestratorService;
import com.hxs.novelagent.novelgraph.service.OrchestratorService.RunResult;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/novelgraph")
public class NovelGraphController {

    private final OrchestratorService orchestratorService;

    public NovelGraphController(OrchestratorService orchestratorService) {
        this.orchestratorService = orchestratorService;
    }

    @PostMapping("/process")
    public ResponseEntity<RunResult> process(@RequestBody(required = false) ProcessRequest request) {
        ProcessRequest req = request == null ? new ProcessRequest(null, false, null, null) : request;
        Path novel = req.novelPath() != null && !req.novelPath().isBlank()
                ? Path.of(req.novelPath())
                : Path.of("/Users/yyinging/Downloads/鸳鸯刀 (金庸) (Z-Library).txt");
        RunResult result = orchestratorService.run(
                novel,
                req.reset(),
                req.model(),
                req.temperature()
        );
        return ResponseEntity.ok(result);
    }

    public record ProcessRequest(String novelPath, boolean reset, String model, Double temperature) {
    }
}
