# 恋爱助手（Love Assistant）

一个基于大语言模型的情感咨询与智能体应用：既能围绕恋爱话题做多轮对话、基于自建知识库回答，
也能把「搜索 / 网页抓取 / 文件操作 / PDF 生成」等工具交给一个会自主规划的智能体去组合执行。

> 技术栈以 Spring AI 为主，同时打通了 MCP 协议（客户端 + 服务端）、RAG 全链路与 SSE 流式输出。

## 功能

**1. AI 情感顾问（LoveApp）**
- 多轮对话，支持内存 / 文件两种会话记忆
- 基于 Markdown 知识库的 RAG 问答（自定义切分、关键词增强、查询改写、上下文补全）
- 自定义 Advisor：请求日志、推理增强
- SSE 流式输出，前端可实时看到回复

**2. 智能助手（可自主规划的 Agent）**
- 手写 ReAct 循环：`思考 → 选择工具 → 执行 → 观察结果 → 继续`
- 内置工具：网页搜索、网页抓取、文件读写、资源下载、终端命令、PDF 生成、终止
- 通过 MCP 协议接入外部能力（高德地图 MCP、自研图片搜索 MCP 服务）
- 单步限流保护（`maxSteps`），避免无限循环

## 技术栈

| 层 | 选型 |
|---|---|
| 语言 / 框架 | Java 21、Spring Boot 3.4、Spring AI 1.0 |
| 模型接入 | Spring AI Alibaba（DashScope / 通义千问）、Ollama（本地模型） |
| 向量存储 | SimpleVectorStore（内存）、PgVector（PostgreSQL） |
| 协议 | MCP（stdio / SSE）、SSE 流式输出 |
| 文档 | Knife4j（OpenAPI 3） |
| 前端 | Vue 3 + Vite |

## 目录结构

```
.
├── src/main/java/com/loveassistant
│   ├── advisor/        # 自定义 Advisor（日志、推理增强）
│   ├── agent/          # ReAct 智能体（BaseAgent / ReActAgent / ToolCallAgent）
│   ├── app/            # 情感顾问应用
│   ├── chatmemory/     # 基于文件的会话记忆（Kryo 序列化）
│   ├── controller/     # 对外 HTTP / SSE 接口
│   ├── rag/            # RAG 配置：文档加载、切分、关键词增强、向量库
│   ├── tools/          # 工具实现与集中注册
├── src/main/resources
│   ├── application.yml
│   ├── mcp-servers.json   # MCP 服务（stdio）启动配置
│   └── document/          # 知识库 Markdown
├── love-assistant-frontend/  # Vue 3 前端
└── image-search-mcp-server/ # 基于 MCP 协议的图片搜索服务（独立 Maven 工程）
```

## 快速开始

### 1. 准备环境

- JDK 21
- Maven 3.9+
- 阿里云百炼 API Key（[获取地址](https://bailian.console.aliyun.com/)）

### 2. 配置

所有密钥都不建议写进代码，推荐用环境变量：

```yaml
spring:
  ai:
    dashscope:
      api-key: ${DASHSCOPE_API_KEY}
```

```powershell
# Windows PowerShell
$env:DASHSCOPE_API_KEY = "sk-xxxx"
```

> 可选：`search-api.api-key`（网页搜索工具）、`AMAP_MAPS_API_KEY`（高德 MCP）、
> `ImageSearchTool` 里的 Pexels Key —— 不配置的话对应工具会返回错误，但不影响主流程启动。

### 3. 启动

```powershell
mvn clean package -DskipTests
java -jar target/love-assistant-0.0.1-SNAPSHOT.jar
```

访问 `http://localhost:8123/api/swagger-ui.html` 查看接口文档，前端项目见 `love-assistant-frontend/`。

### 4. 使用 MCP 服务

`src/main/resources/mcp-servers.json` 里配置了两个 stdio MCP 服务，其中图片搜索服务需要先打包：

```powershell
cd image-search-mcp-server
.\mvnw.cmd -DskipTests package
```

## 测试

`mvn test` 默认只跑不依赖外部环境的单测，克隆下来即可执行：

| 测试 | 覆盖内容 |
|---|---|
| `FileOperationToolTest` | 写入后读回内容一致、覆盖写、文件不存在时的降级返回 |
| `PDFGenerationToolTest` | PDF 真实落盘、非空、以 `%PDF` 文件头开头、非法文件名降级 |
| `WebSearchToolTest` | 注入假 HTTP 响应，覆盖请求参数拼装、最多 5 条截断、结果不足 5 条、空结果、调用异常降级 |
| `TerminalOperationToolTest` | 终端命令执行与非零退出码处理 |
| `ReActLoopTest` | 智能体循环行为：回答不被丢弃、答完立即停止、多步不提前结束、旁白保留、无回答时兜底总结 |

以下测试依赖真实环境（模型 API Key / 外网 / PostgreSQL），已从默认执行中排除，
需要时把 `pom.xml` 里 surefire 的 `excludes` 对应项注释掉再跑：
`LoveAssistantApplicationTests`、`LoveAppTest`、`LoveAgentTest`、`LoveAppDocumentLoaderTest`、
`PgVectorVectorStoreConfigTest`、`ResourceDownloadToolTest`、`WebScrapingToolTest`。

## 核心实现说明

**RAG 链路**：`LoveAppDocumentLoader` 读取 Markdown → `TokenTextSplitter` 切分 →
`KeywordMetadataEnricher` 补充关键词 → 写入向量库 → `QueryRewriter` 改写用户问题 →
`QuestionAnswerAdvisor` 检索并拼装上下文。

**Agent 循环**：`BaseAgent` 负责状态机与步数控制，`ReActAgent` 定义 `think/act` 模板方法，
`ToolCallAgent` 实现工具调用（`ToolCallingManager.executeToolCalls`），`LoveAgent` 注入系统提示词与步数上限。

**自定义 Advisor**：`MyLoggerAdvisor` 打印请求与响应，`ReReadingAdvisor` 通过二次阅读指令提升推理质量。

## 已知限制

- 会话记忆默认存内存，重启丢失（`FileBasedChatMemory` 为文件方案示例）
- 工具实现为演示级：搜索/抓取工具未做重试与超时治理，终端工具会直接执行命令，请勿用于生产
- 未做鉴权、限流与成本控制
