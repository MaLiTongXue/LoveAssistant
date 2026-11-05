package com.loveassistant.tools;

import cn.hutool.json.JSONArray;
import cn.hutool.json.JSONObject;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 网页搜索工具单测：注入假的 HTTP 响应，离线验证参数拼装与结果解析逻辑。
 */
class WebSearchToolTest {

    /** 构造 n 条搜索结果的假响应 */
    private static String responseWith(int n) {
        JSONArray results = new JSONArray();
        for (int i = 1; i <= n; i++) {
            results.add(new JSONObject()
                    .set("title", "恋爱沟通技巧-" + i)
                    .set("link", "https://example.com/" + i));
        }
        return new JSONObject().set("organic_results", results).toString();
    }

    private static int countOccurrences(String text, String token) {
        int count = 0;
        int index = text.indexOf(token);
        while (index >= 0) {
            count++;
            index = text.indexOf(token, index + token.length());
        }
        return count;
    }

    @Test
    @DisplayName("请求参数应包含 q / api_key / engine，且最多返回 5 条结果")
    void shouldForwardParametersAndLimitResults() {
        AtomicReference<Map<String, Object>> captured = new AtomicReference<>();
        WebSearchTool tool = new WebSearchTool("test-api-key", params -> {
            captured.set(params);
            return responseWith(6);
        });

        String result = tool.searchWeb("恋爱沟通技巧");

        Map<String, Object> params = captured.get();
        assertEquals("恋爱沟通技巧", params.get("q"));
        assertEquals("test-api-key", params.get("api_key"));
        assertEquals("baidu", params.get("engine"));

        assertEquals(5, countOccurrences(result, "\"title\""), "最多只应返回 5 条：" + result);
        assertTrue(result.contains("恋爱沟通技巧-1"));
        assertFalse(result.contains("恋爱沟通技巧-6"), "第 6 条应被截断");
    }

    @Test
    @DisplayName("结果少于 5 条时不应越界（回归：原来的 subList(0,5) 会抛异常）")
    void shouldHandleFewerResultsThanLimit() {
        WebSearchTool tool = new WebSearchTool("k", params -> responseWith(2));

        String result = tool.searchWeb("只有两条结果");

        assertFalse(result.startsWith("Error searching Baidu:"), "不应走到异常分支：" + result);
        assertEquals(2, countOccurrences(result, "\"title\""), result);
    }

    @Test
    @DisplayName("没有任何结果时应返回提示语，而不是异常信息")
    void shouldReturnFriendlyMessageWhenNoResults() {
        WebSearchTool tool = new WebSearchTool("k", params -> "{\"organic_results\":[]}");

        assertEquals("No search result found for: 查不到", tool.searchWeb("查不到"));
    }

    @Test
    @DisplayName("HTTP 调用抛异常时应降级为错误信息，而不是让调用方崩溃")
    void shouldDegradeGracefullyWhenHttpFails() {
        WebSearchTool tool = new WebSearchTool("k", params -> {
            throw new IllegalStateException("connection refused");
        });

        String result = tool.searchWeb("任意查询");

        assertTrue(result.startsWith("Error searching Baidu:"), result);
        assertTrue(result.contains("connection refused"), result);
    }
}
