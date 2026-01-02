package com.hxs.novelagent.novelgraph.persistence;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface ProcessedChunkRepository extends JpaRepository<ProcessedChunkEntity, String> {

    @Query("select p.chunkId from ProcessedChunkEntity p")
    List<String> findAllChunkIds();
}
