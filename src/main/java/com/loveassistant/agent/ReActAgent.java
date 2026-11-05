package com.loveassistant.agent;

import cn.hutool.core.util.StrUtil;
import com.loveassistant.agent.model.AgentState;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.extern.slf4j.Slf4j;

/**
 * ReAct (Reasoning and Acting) 模式的代理抽象类
 * 实现了思考-行动的循环模式
 */
@EqualsAndHashCode(callSuper = true)
@Data
@Slf4j
public abstract class ReActAgent extends BaseAgent {

    /**
     * 处理当前状态并决定下一步行动
     *
     * @return 是否需要执行行动，true表示需要执行，false表示不需要执行
     */
    public abstract boolean think();

    /**
     * 执行决定的行动
     *
     * @return 行动执行结果
     */
    public abstract String act();

    /**
     * 执行单个步骤：思考和行动
     *
     * @return 步骤执行结果
     */
    @Override
    public String step() {
        try {
            // 先思考
            boolean shouldAct = think();
            if (!shouldAct) {
                // 模型选择不再调用工具，说明已经给出最终回答：
                // 这里必须结束循环并把回答交还，否则回答会被丢掉、循环还会继续空转到最大步数
                setState(AgentState.FINISHED);
                String answer = getLastAssistantText();
                return StrUtil.isBlank(answer) ? "思考完成 - 无需行动" : answer;
            }
            // 再行动
            return act();
        } catch (Exception e) {
            // 记录异常日志
            log.error("步骤执行失败", e);
            return "步骤执行失败：" + e.getMessage();
        }
    }

}
