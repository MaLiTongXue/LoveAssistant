package com.loveassistant.agent;

import com.loveassistant.advisor.MyLoggerAdvisor;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.stereotype.Component;

/**
 * 可自主规划的智能体（ReAct 模式，支持工具调用）
 */
@Component
public class LoveAgent extends ToolCallAgent {

    public LoveAgent(ToolCallback[] allTools, ChatModel dashscopeChatModel) {
        super(allTools);
        this.setName("LoveAgent");
        String SYSTEM_PROMPT = """
                You are LoveAgent, a capable AI assistant that solves the user's task, calling tools only when needed.
                Rules you must follow:
                1. If you already have enough information, answer the user directly in plain language and stop.
                2. Call a tool only when it is genuinely required to get new information or to perform an action.
                3. Every task must end with a clear natural-language answer to the user.
                   Never end a task with tool calls alone.
                """;
        this.setSystemPrompt(SYSTEM_PROMPT);
        String NEXT_STEP_PROMPT = """
                Decide the next move for the user's request.
                - Need more facts first? Call the single most appropriate tool.
                - Already have enough? Reply to the user directly in plain language, without calling any tool.
                  That plain reply IS the final answer, so do not call the terminate tool instead of answering.
                Base every claim on the tool output you actually received; say so if the data was insufficient.
                """;
        this.setNextStepPrompt(NEXT_STEP_PROMPT);
        this.setMaxSteps(20);
        // 初始化 AI 对话客户端
        ChatClient chatClient = ChatClient.builder(dashscopeChatModel)
                .defaultAdvisors(new MyLoggerAdvisor())
                .build();
        this.setChatClient(chatClient);
    }
}
