package com.loveassistant.agent;

import com.loveassistant.agent.model.AgentState;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * ReAct 循环回归测试（离线，不依赖模型/网络）：
 * 1) 模型给出最终回答（不再调用工具）时，回答必须被返回，而不是只返回占位文案；
 * 2) 拿到回答后循环必须立刻结束，不能继续空转直到 maxSteps。
 */
class ReActLoopTest {

    /** 前 actionSteps 步返回"需要调用工具"，之后给出最终回答 */
    private static class StubAgent extends ReActAgent {

        private final int actionSteps;

        private int thinkCount = 0;

        StubAgent(int actionSteps) {
            this.actionSteps = actionSteps;
            setName("StubAgent");
            setMaxSteps(20);
        }

        @Override
        public boolean think() {
            thinkCount++;
            if (thinkCount <= actionSteps) {
                setLastAssistantText("准备调用工具");
                return true;
            }
            setLastAssistantText("上海今天多云，18~24℃，适合出行。");
            return false;
        }

        @Override
        public String act() {
            return "已调用工具 searchWeb";
        }

        int thinkCount() {
            return thinkCount;
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
    @DisplayName("模型既不给回答也不调用工具时，循环应结束且给出兜底文案")
    void shouldFallBackWhenNothingProduced() {
        StubAgent agent = new StubAgent(0) {
            @Override
            public boolean think() {
                setLastAssistantText("");
                return false;
            }
        };

        String result = agent.run("空响应");

        assertEquals(AgentState.FINISHED, agent.getState());
        assertTrue(result.contains("思考完成 - 无需行动"), result);
    }
}
