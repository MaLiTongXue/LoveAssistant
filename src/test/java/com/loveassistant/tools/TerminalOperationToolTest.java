package com.loveassistant.tools;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 终端工具单测：校验输出捕获与退出码处理。
 */
class TerminalOperationToolTest {

    private final TerminalOperationTool terminalOperationTool = new TerminalOperationTool();

    @Test
    @DisplayName("执行成功时应返回真实输出，且不含错误提示")
    void executeTerminalCommandShouldCaptureOutput() {
        String result = terminalOperationTool.executeTerminalCommand("dir");

        assertFalse(result.isBlank(), "命令有输出，不应返回空字符串");
        assertFalse(result.contains("Error executing command"), result);
        assertFalse(result.contains("Command execution failed with exit code"), result);
    }

    @Test
    @DisplayName("命令返回非零退出码时应把退出码写进返回值")
    void executeTerminalCommandShouldReportNonZeroExitCode() {
        String result = terminalOperationTool.executeTerminalCommand("exit 3");

        assertTrue(result.contains("Command execution failed with exit code: 3"), result);
    }
}
