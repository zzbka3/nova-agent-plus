package com.cs.online.model;

import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.data.message.SystemMessage;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.chat.response.ChatResponse;
import dev.langchain4j.model.openai.OpenAiChatModel;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * ModelRuntime 封装 LangChain4j 的 OpenAI 兼容 ChatModel 调用，供 AgentRuntime 的 Planner 做 ReAct 决策。
 */
@Component
@EnableConfigurationProperties(ModelProperties.class)
public class ModelRuntime {

    private final ChatModel chatModel;

    public ModelRuntime(ModelProperties properties) {
        this.chatModel = OpenAiChatModel.builder()
                .baseUrl(properties.getBaseUrl())
                .apiKey(properties.getApiKey())
                .modelName(properties.getModelName())
                .build();
    }

    public String chat(List<ChatMessage> messages) {
        ChatResponse response = chatModel.chat(messages);
        return response.aiMessage().text();
    }

    public String chat(String systemPrompt, String userMessage) {
        return chat(List.of(
                SystemMessage.from(systemPrompt),
                UserMessage.from(userMessage)
        ));
    }
}
