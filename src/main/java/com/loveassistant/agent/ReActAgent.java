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
                String answer = getLastAssistantText();
                setState(AgentState.FINISHED);
                if (StrUtil.isBlank(answer)) {
                    // 模型既没给回答也没调工具：交给 BaseAgent 的兜底总结处理
                    return "思考完成 - 无需行动";
                }
                setAnswered(true);
                return answer;
            }
            // 再行动
            String actionResult = act();
            String narration = getLastAssistantText();
            // 模型调用工具时通常会顺带说明自己在做什么，这段旁白也应展示给用户
            return StrUtil.isBlank(narration) ? actionResult : narration + "\n" + actionResult;
        } catch (Exception e) {
            // 记录异常日志
            log.error("步骤执行失败", e);
            return "步骤执行失败：" + e.getMessage();
        }
    }

}
