package com.hxs.novelagent.novelgraph.model;

import java.util.ArrayList;
import java.util.List;

public class ChunkSummary {
    private String content;
    private List<String> mainCharacters = new ArrayList<>();

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public List<String> getMainCharacters() {
        return mainCharacters;
    }

    public void setMainCharacters(List<String> mainCharacters) {
        this.mainCharacters = mainCharacters == null ? new ArrayList<>() : new ArrayList<>(mainCharacters);
    }
}
