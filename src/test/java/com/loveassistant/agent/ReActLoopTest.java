package com.loveassistant.agent;

import com.loveassistant.agent.model.AgentState;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * ReAct 循环回归测试（离线，不依赖模型/网络），钉住四条行为：
 * 1) 模型给出最终回答（不再调用工具）时必须原样返回，不能用占位文案顶替；
 * 2) 拿到回答后循环立刻结束，不能空转到 maxSteps；
 * 3) 模型调用工具时的旁白也要展示，不能只剩工具名；
 * 4) 整轮没有回答（例如一路调工具后直接终止）时，必须补一次兜底总结。
 */
class ReActLoopTest {

    /** 前 actionSteps 步返回"需要调用工具"，之后给出最终回答 */
    private static class StubAgent extends ReActAgent {

        private final int actionSteps;

        private int thinkCount = 0;

        private int finalizeCount = 0;

        private String narration = "准备调用工具";

        private String fallbackAnswer = null;

        StubAgent(int actionSteps) {
            this.actionSteps = actionSteps;
            setName("StubAgent");
            setMaxSteps(20);
        }

        @Override
        public boolean think() {
            thinkCount++;
            if (thinkCount <= actionSteps) {
                setLastAssistantText(narration);
                return true;
            }
            setLastAssistantText("上海今天多云，18~24℃，适合出行。");
            return false;
        }

        @Override
        public String act() {
            return "已调用工具 searchWeb";
        }

        @Override
        protected String finalizeAnswer() {
            finalizeCount++;
            return fallbackAnswer;
        }

        int thinkCount() {
            return thinkCount;
        }

        int finalizeCount() {
            return finalizeCount;
        }

        StubAgent withNarration(String narration) {
            this.narration = narration;
            return this;
        }

        StubAgent withFallbackAnswer(String answer) {
            this.fallbackAnswer = answer;
            return this;
        }
    }

    @Test
    @DisplayName("模型给出最终回答后，该回答必须被原样返回，而不是被占位文案丢掉")
    void shouldReturnFinalAnswerInsteadOfPlaceholder() {
        StubAgent agent = new StubAgent(1);

        String result = agent.run("帮我查一下上海的天气");

        assertTrue(result.contains("上海今天多云"), "应返回模型的实际回答，实际为：" + result);
        assertFalse(result.contains("思考完成 - 无需行动"), "不应只返回占位文案：" + result);
    }

    @Test
    @DisplayName("拿到回答后应立即结束循环（1 次工具调用 + 1 次回答 = 2 步），而不是空转到 maxSteps")
    void shouldStopLoopAfterFinalAnswer() {
        StubAgent agent = new StubAgent(1);

        agent.run("帮我查一下上海的天气");

        assertEquals(2, agent.thinkCount(), "应只思考 2 次，实际：" + agent.thinkCount());
        assertEquals(AgentState.FINISHED, agent.getState());
    }

    @Test
    @DisplayName("需要连续调用工具时，中间步骤不能提前结束")
    void shouldKeepGoingWhileToolsAreStillNeeded() {
        StubAgent agent = new StubAgent(3);

        String result = agent.run("多步任务");

        assertEquals(4, agent.thinkCount(), "3 次工具调用 + 1 次回答");
        assertTrue(result.contains("上海今天多云"), result);
        assertTrue(result.contains("已调用工具 searchWeb"), "中间步骤应保留工具调用记录：" + result);
    }

    @Test
    @DisplayName("模型调用工具时写的旁白要和进度一起展示，不能只剩工具名")
    void shouldKeepNarrationWhenCallingTool() {
        StubAgent agent = new StubAgent(1).withNarration("我先搜索一下上海的天气");

        String result = agent.run("帮我查一下上海的天气");

        assertTrue(result.contains("我先搜索一下上海的天气"), "旁白丢失：" + result);
        assertTrue(result.contains("已调用工具 searchWeb"), result);
    }

    @Test
    @DisplayName("整轮没有任何回答（一路调工具后直接终止）时，必须补一次兜底总结")
    void shouldFallBackToFinalizeAnswerWhenNeverAnswered() {
        StubAgent agent = new StubAgent(99) {
            @Override
            public boolean think() {
                setLastAssistantText("正在终止");
                return true;
            }

            @Override
            public String act() {
                // 模拟模型直接调用终止工具
                setState(AgentState.FINISHED);
                return "已调用工具 doTerminate";
            }
        }.withFallbackAnswer("上海今天多云，18~24℃，适合出行。");

        String result = agent.run("帮我查一下上海的天气");

        assertEquals(1, agent.finalizeCount(), "没有回答时必须触发兜底总结");
        assertTrue(result.contains("上海今天多云"), "用户应能看到兜底总结：" + result);
    }

    @Test
    @DisplayName("已经产出回答时不应再多花一次兜底调用")
    void shouldNotCallFinalizeAnswerWhenAlreadyAnswered() {
        StubAgent agent = new StubAgent(1).withFallbackAnswer("不该出现");

        agent.run("帮我查一下上海的天气");

        assertEquals(0, agent.finalizeCount(), "已有回答时不应再调用兜底");
    }

    @Test
    @DisplayName("模型既不给回答也不调用工具时，循环应结束并走兜底")
    void shouldFallBackWhenNothingProduced() {
        StubAgent agent = new StubAgent(0) {
            @Override
            public boolean think() {
                setLastAssistantText("");
                return false;
            }
        }.withFallbackAnswer("抱歉，没有获取到有效信息。");

        String result = agent.run("空响应");

        assertEquals(AgentState.FINISHED, agent.getState());
        assertTrue(result.contains("抱歉，没有获取到有效信息。"), result);
    }
}
