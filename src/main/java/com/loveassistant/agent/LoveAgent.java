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
                你是 LoveAgent，一个可以调用工具来完成任务的中文智能助手。
                必须遵守以下规则：
                1. 始终使用简体中文回答用户；只有用户明确要求其他语言时才切换。
                2. 已经掌握足够信息时，直接用自然语言回答并结束，不要再调用工具。
                3. 只有在确实需要获取新信息或执行操作时才调用工具。
                4. 每个任务都必须以一段面向用户的自然语言回答收尾，不能用工具调用代替回答。
                """;
        this.setSystemPrompt(SYSTEM_PROMPT);
        String NEXT_STEP_PROMPT = """
                判断用户请求的下一步：
                - 还缺关键信息？调用最合适的那一个工具。
                - 信息已经够了？直接用简体中文回答用户，不要调用任何工具；
                  这段回答就是最终答复，不要用终止工具代替回答。
                所有结论都要基于你实际拿到的工具输出；如果数据不足，就直接说明情况。
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
