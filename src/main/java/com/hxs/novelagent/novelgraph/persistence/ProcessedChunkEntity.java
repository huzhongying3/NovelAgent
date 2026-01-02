package com.hxs.novelagent.novelgraph.persistence;

import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "novelgraph_processed_chunk")
public class ProcessedChunkEntity {

    @Id
    @Column(name = "chunk_id", nullable = false, length = 128)
    private String chunkId;

    @Column(name = "processed_at", nullable = false)
    private LocalDateTime processedAt;

    protected ProcessedChunkEntity() {
    }

    public ProcessedChunkEntity(String chunkId, LocalDateTime processedAt) {
        this.chunkId = chunkId;
        this.processedAt = processedAt;
    }

    public String getChunkId() {
        return chunkId;
    }

    public LocalDateTime getProcessedAt() {
        return processedAt;
    }
}
