package com.hxs.novelagent.novelgraph.service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;

public class TextIngestionService {

    private final int chunkSize;
    private final int chunkOverlap;
    private final List<String> separators;

    public TextIngestionService(int chunkSize, int chunkOverlap) {
        this.chunkSize = chunkSize;
        this.chunkOverlap = chunkOverlap;
        this.separators = List.of("\n\n", "\n", "。", "！", "？", "，", " ");
    }

    public List<TextChunk> ingest(Path filePath) {
        try {
            String text = Files.readString(filePath);
            return split(text);
        } catch (IOException e) {
            throw new IllegalArgumentException("读取文本失败: " + filePath, e);
        }
    }

    private List<TextChunk> split(String text) {
        List<String> segments = recursiveSplit(text, chunkSize, separators);
        List<TextChunk> chunks = new ArrayList<>();
        int idx = 0;
        for (String seg : segments) {
            String cleaned = seg.strip();
            if (cleaned.isEmpty()) {
                continue;
            }
            String chunkId = HashUtils.hash(cleaned);
            chunks.add(new TextChunk(chunkId, cleaned, idx++));
        }
        return chunks;
    }

    private List<String> recursiveSplit(String text, int target, List<String> seps) {
        if (text.length() <= target || seps.isEmpty()) {
            return List.of(text);
        }
        String sep = seps.get(0);
        String[] parts = text.split(Pattern.quote(sep));
        List<String> acc = new ArrayList<>();
        StringBuilder buf = new StringBuilder();
        for (String part : parts) {
            if (buf.length() + part.length() + sep.length() <= target) {
                if (!buf.isEmpty()) {
                    buf.append(sep);
                }
                buf.append(part);
            } else {
                if (!buf.isEmpty()) {
                    acc.add(buf.toString());
                }
                buf = new StringBuilder(part);
            }
        }
        if (!buf.isEmpty()) {
            acc.add(buf.toString());
        }

        List<String> result = new ArrayList<>();
        for (String a : acc) {
            if (a.length() > target && seps.size() > 1) {
                result.addAll(recursiveSplit(a, target, seps.subList(1, seps.size())));
            } else {
                result.add(a);
            }
        }

        // add overlaps
        if (chunkOverlap > 0) {
            List<String> withOverlap = new ArrayList<>();
            for (int i = 0; i < result.size(); i++) {
                String curr = result.get(i);
                if (i > 0 && chunkOverlap < curr.length()) {
                    String prevTail = result.get(i - 1);
                    String overlap = prevTail.substring(Math.max(0, prevTail.length() - chunkOverlap));
                    curr = overlap + curr;
                }
                withOverlap.add(curr);
            }
            return withOverlap;
        }
        return result;
    }

    public record TextChunk(String chunkId, String text, int index) {
    }

    private static class HashUtils {
        private static final java.security.MessageDigest MD5;

        static {
            try {
                MD5 = java.security.MessageDigest.getInstance("MD5");
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }

        static String hash(String text) {
            byte[] bytes = MD5.digest(text.getBytes(java.nio.charset.StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder();
            for (byte b : bytes) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString();
        }
    }
}
