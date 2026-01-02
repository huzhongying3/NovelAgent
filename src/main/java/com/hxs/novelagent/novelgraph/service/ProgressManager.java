package com.hxs.novelagent.novelgraph.service;

import java.util.HashSet;
import java.util.Set;

import com.hxs.novelagent.novelgraph.persistence.ProcessedChunkEntity;
import com.hxs.novelagent.novelgraph.persistence.ProcessedChunkRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class ProgressManager {

    private final ProcessedChunkRepository repository;
    private Set<String> processed;

    public ProgressManager(ProcessedChunkRepository repository) {
        this.repository = repository;
        this.processed = load();
    }

    public boolean isProcessed(String chunkId) {
        return processed.contains(chunkId);
    }

    public void mark(String chunkId) {
        if (chunkId == null || chunkId.isBlank()) {
            return;
        }
        if (processed.add(chunkId)) {
            repository.save(new ProcessedChunkEntity(chunkId, java.time.LocalDateTime.now()));
        }
    }

    public void reset() {
        processed = new HashSet<>();
        repository.deleteAllInBatch();
    }

    private Set<String> load() {
        return new HashSet<>(repository.findAllChunkIds());
    }
}
