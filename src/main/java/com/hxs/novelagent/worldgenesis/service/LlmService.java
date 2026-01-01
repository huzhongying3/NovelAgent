package com.hxs.novelagent.worldgenesis.service;

import java.util.List;

import com.hxs.novelagent.worldgenesis.model.WorldBible;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.openai.OpenAiChatOptions;
import org.springframework.ai.openai.api.OpenAiApi.ChatCompletionRequest.ResponseFormat;
import org.springframework.stereotype.Service;

@Service
public class LlmService {

    private static final Logger log = LoggerFactory.getLogger(LlmService.class);

    private static final String SYSTEM_PROMPT = """
            角色：计算社会学家 & 剧本分析师。
            任务：基于小说片段进行归纳推理，逆向工程出《世界设定集》。
            
            分析要求（必须执行）：
            - 寻找代价：力量体系必须指出限制/代价（power_limitations），如耗内力、反噬、资源昂贵。
            - 挖掘矛盾：明确谁与谁冲突、为何冲突，写入 world_conflict_source。
            - 保留术语：将兵器名、武功名、专有名词写入 specific_terminology，保持原文。
            - 核心势力：列出主要势力/阵营（major_factions）。
            
            输出格式规则（语言强制）：
            - JSON Keys：固定为 power_system, power_limitations, geographical_scope, social_hierarchy, world_conflict_source, major_factions（数组）, economic_rules, cultural_taboos, specific_terminology（数组）。
            - JSON Values：必须用【简体中文】，专有名词保持原文（如“周威信”“鸳鸯刀”）。
            - 仅输出 JSON 字符串，不要额外说明。
            
            约束：
            - 严格基于提供文本，不要过度联想；如果文本提到“银两”，不要泛化为“金币”。
            """;

    private final ChatModel chatModel;
    private final WorldBibleNormalizer normalizer;

    public LlmService(ChatModel chatModel, WorldBibleNormalizer normalizer) {
        this.chatModel = chatModel;
        this.normalizer = normalizer;
    }

    public WorldBible analyzeWorldRules(String contextText) {
        Prompt prompt = new Prompt(
                List.of(
                        new SystemMessage(SYSTEM_PROMPT),
                        new UserMessage(contextText)
                ),
                OpenAiChatOptions.builder()
                        .withTemperature(0.0f)
                        .withResponseFormat(new ResponseFormat(ResponseFormat.Type.JSON_OBJECT))
                        .build()
        );

        ChatResponse response = chatModel.call(prompt);
        String content = response.getResult().getOutput().getContent();
        if (content == null || content.isBlank()) {
            throw new IllegalStateException("LLM 返回空内容，请重试或检查上下文。");
        }

        try {
            return normalizer.normalize(content);
        } catch (IllegalArgumentException ex) {
            log.error("LLM 输出无法解析: {}", content);
            throw new IllegalStateException("LLM 输出无法解析为 WorldBible", ex);
        }
    }
}
