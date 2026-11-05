package com.loveassistant.tools;

import org.springframework.ai.tool.annotation.Tool;

/**
 * 终止工具（作用是让自主规划智能体能够合理地中断）
 */
public class TerminateTool {

    @Tool(description = """
            End the interaction early. Use it ONLY when the task truly cannot proceed
            (for example a missing API key makes every tool fail).
            Never use it instead of answering the user: producing a plain-language answer
            is how a task normally finishes.
            """)
    public String doTerminate() {
        return "任务结束";
    }
}
