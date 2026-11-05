package com.loveassistant.tools;

import cn.hutool.http.HttpUtil;
import cn.hutool.json.JSONArray;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 网页搜索工具
 */
public class WebSearchTool {

    // SearchAPI 的搜索接口地址
    private static final String SEARCH_API_URL = "https://www.searchapi.io/api/v1/search";

    // 最多返回的结果条数
    private static final int MAX_RESULTS = 5;

    private final String apiKey;

    // HTTP 调用实现，抽成函数是为了让单测可以离线注入假响应
    private final Function<Map<String, Object>, String> httpGetter;

    public WebSearchTool(String apiKey) {
        this(apiKey, params -> HttpUtil.get(SEARCH_API_URL, params));
    }

    WebSearchTool(String apiKey, Function<Map<String, Object>, String> httpGetter) {
        this.apiKey = apiKey;
        this.httpGetter = httpGetter;
    }

    @Tool(description = "Search for information from Baidu Search Engine")
    public String searchWeb(
            @ToolParam(description = "Search query keyword") String query) {
        Map<String, Object> paramMap = new HashMap<>();
        paramMap.put("q", query);
        paramMap.put("api_key", apiKey);
        paramMap.put("engine", "baidu");
        try {
            String response = httpGetter.apply(paramMap);
            JSONObject jsonObject = JSONUtil.parseObj(response);
            // 提取 organic_results 部分
            JSONArray organicResults = jsonObject.getJSONArray("organic_results");
            if (organicResults == null || organicResults.isEmpty()) {
                return "No search result found for: " + query;
            }
            // 最多取前 MAX_RESULTS 条，结果不足时自动缩短，避免越界
            List<Object> objects = organicResults.subList(0, Math.min(MAX_RESULTS, organicResults.size()));
            // 拼接搜索结果为字符串
            return objects.stream().map(obj -> {
                JSONObject tmpJSONObject = (JSONObject) obj;
                return tmpJSONObject.toString();
            }).collect(Collectors.joining(","));
        } catch (Exception e) {
            return "Error searching Baidu: " + e.getMessage();
        }
    }
}
