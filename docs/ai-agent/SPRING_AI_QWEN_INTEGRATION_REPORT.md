# Spring AI + 通义千问基础接入报告

## 1. 实施概况

| 项目 | 结果 |
|---|---|
| Spring Boot | 3.5.16 |
| Java | 17 |
| Spring AI | 1.1.8 |
| 模型服务 | 阿里云百炼 OpenAI 兼容 API |
| 模型 | `qwen-plus` |
| 最终 Chat Completions 地址 | `https://dashscope.aliyuncs.com/compatible-mode/v1/chat/completions` |
| 当前验收状态 | **PASS** |

Spring AI 依赖、配置隔离、`ChatClient`、固定非敏感 Java Tool、异常处理和自动化回归均已完成。2026-10-10 已通过显式集成测试完成真实 Qwen 文本响应和真实 Qwen Tool Calling 验收。

## 2. 修改文件清单

- `backend/pom.xml`：导入 Spring AI 1.1.8 BOM，并引入 OpenAI 模型 Starter。
- `backend/src/main/resources/application.yml`：普通环境默认关闭 Chat/Embedding 模型，避免无密钥环境和普通测试触发模型自动配置。
- `backend/src/main/resources/application-ai.yml`：增加仅在 `ai` Profile 生效的百炼 OpenAI 兼容配置。
- `backend/src/main/java/com/company/crm/ai/config/AiChatClientConfig.java`：基于自动配置的 `ChatClient.Builder` 创建具名 Qwen 客户端，不在启动时调用模型。
- `backend/src/main/java/com/company/crm/ai/smoke/QwenSmokeService.java`：提供无 HTTP 暴露的文本和 Tool Calling 显式冒烟入口。
- `backend/src/main/java/com/company/crm/ai/smoke/QwenSmokeTool.java`：提供固定、只读、无数据库访问的 `getCrmSystemInfo` 工具及执行计数证据。
- `backend/src/main/java/com/company/crm/ai/smoke/QwenSmokeException.java`：统一冒烟调用异常，避免向上层暴露远端错误细节。
- `backend/src/test/java/com/company/crm/ai/config/AiChatClientConfigTest.java`：验证 Profile 隔离和具名 `ChatClient` Bean。
- `backend/src/test/java/com/company/crm/ai/smoke/QwenSmokeToolTest.java`：验证固定结果、`@Tool` 元数据、Spring AI `ToolCallback` 注册与 Java 方法执行。
- `backend/src/test/java/com/company/crm/ai/smoke/QwenSmokeServiceTest.java`：验证文本响应、空响应、客户端异常及模型未选工具路径。
- `backend/src/test/java/com/company/crm/ai/smoke/QwenLiveIntegrationTest.java`：与普通构建隔离的真实 Qwen 文本及 Tool Calling 测试。
- `docs/CODEX_CONTEXT.md`、`docs/ai-agent/AI_AGENT_V1_TECH_STACK.md`：同步长期技术事实和当前验收状态。

未修改前端、数据库结构、数据库数据、JWT、RBAC、`DataScopeResolver` 或现有 CRM 业务代码。

## 3. Maven 依赖

通过 `dependencyManagement` 导入：

```text
org.springframework.ai:spring-ai-bom:1.1.8
```

业务依赖仅声明：

```text
org.springframework.ai:spring-ai-starter-model-openai
```

Starter 未重复声明版本。Maven 已成功解析 `spring-ai-starter-model-openai:1.1.8`、OpenAI 模型、ChatClient、Tool Calling 和相关自动配置依赖。未引入 LangChain4j、Spring AI Alibaba Agent Framework、RAG、Vector Store、MCP、Redis 或 Python 服务。

## 4. 配置方案

普通环境在 `application.yml` 中将 Chat 和 Embedding 模型设为 `none`。只有显式启用 `ai` Profile 时，`application-ai.yml` 才会：

- 从 `${DASHSCOPE_API_KEY}` 读取 API Key；
- 将 base URL 设为 `https://dashscope.aliyuncs.com/compatible-mode`；
- 将 completions path 设为 `/v1/chat/completions`；
- 使用 `qwen-plus` 和温度 `0.2`；
- 将 Chat 模型切换为 `openai`，Embedding 继续关闭。

该组合最终指向百炼兼容接口，不会出现 `/v1/v1/`。配置文件中没有真实 API Key，测试和报告也未读取或输出密钥值。

## 5. ChatClient 验证结果

- Spring AI 1.1.8 API 和配置元数据已根据实际依赖 JAR 核对。
- `ChatClient.Builder` 由 Starter 自动配置提供，`AiChatClientConfig` 只执行 `builder.build()`。
- 应用启动阶段不发起模型请求。
- 本地 Mock 测试已验证非空响应、空响应和模型客户端异常处理。
- 真实 Qwen 文本调用：**通过**。显式测试成功获得非空文本响应。

## 6. Tool Calling 验证结果

`getCrmSystemInfo()` 固定返回：

```json
{
  "systemName": "CRM",
  "systemType": "Customer Relationship Management",
  "aiIntegration": "Spring AI"
}
```

自动化测试确认：

- `@Tool` 名称为 `getCrmSystemInfo`；
- Spring AI `MethodToolCallbackProvider` 能发现并注册该方法；
- 经 Spring AI `ToolCallback.call("{}")` 能实际执行 Java 方法；
- 调用计数从 0 增至 1，可作为结构化执行证据；
- 工具没有数据库、当前用户、JWT、API Key 或外部业务系统依赖。

真实 Qwen 已完成“Qwen → Spring AI → Java Tool → Qwen”闭环。集成测试比较工具调用前后计数并断言增量大于 0，因此不是仅凭最终回答内容判断成功。

## 7. 自动化测试与构建

执行结果：

```text
mvn clean test
Tests run: 223, Failures: 0, Errors: 0, Skipped: 1
BUILD SUCCESS
```

跳过的 1 项为 `QwenLiveIntegrationTest`，这是预期隔离行为。新增本地 AI 测试共 8 项，全部通过。

显式真实模型测试结果：

```text
mvn.cmd "-Dtest=QwenLiveIntegrationTest" test
Tests run: 1, Failures: 0, Errors: 0, Skipped: 0
BUILD SUCCESS
```

```text
mvn -DskipTests package
BUILD SUCCESS
```

已成功生成可执行 JAR。完整测试中的 Spring MVC 上下文正常启动；未额外启动连接真实数据库的长期运行进程，以避免 `DataInitializer` 对本地数据库产生潜在写入。

真实集成测试需在能读取用户环境变量的新 PowerShell/IDE 进程中显式执行：

```powershell
$env:RUN_QWEN_INTEGRATION = 'true'
cd D:\MyProjectPractice\crm-system\backend
mvn.cmd "-Dtest=QwenLiveIntegrationTest" test
```

命令不会设置或输出 `DASHSCOPE_API_KEY`；该变量必须已由用户安全配置并对当前进程可见。

## 8. CRM 兼容性

- 原有认证、JWT、RBAC、角色边界、对象范围授权和数据权限代码均未修改。
- 原有 Controller、Service、Mapper 测试继续通过。
- MyBatis-Plus 相关上下文和既有 Web 测试正常。
- AI 组件受 `ai` Profile 隔离；普通本地开发、CI 和 `mvn test` 不需要百炼密钥，也不会产生模型费用。
- 没有新增公开或生产测试 HTTP 接口。

## 9. 风险与后续建议

- 在真实验收前，重新打开 IDE、终端或 Codex 运行环境，确认其能看到用户级 `DASHSCOPE_API_KEY`，但不要打印变量值。
- 百炼调用仍需考虑费用、429 限流、网络超时、401/5xx 和重试放大问题；本阶段只提供最小异常边界，未设计生产重试策略。
- 后续业务 Tool 必须在 Java 后端独立执行认证、功能权限和 ALL/DEPT/SELF 数据范围检查，禁止让 LLM 或前端决定授权范围。
- 后续日志只记录脱敏的工具名、状态和耗时，不记录密钥、Authorization、客户明细或完整工具负载。
- 在真实文本和 Tool Calling 均验证通过前，不应把当前基础设施描述为完整 CRM AI Agent。

## 10. 最终结论

**PASS**

- Spring AI 1.1.8 集成：通过。
- Maven 编译、测试、打包：通过。
- 普通无密钥环境隔离：通过。
- Spring AI Tool 注册和本地 Java 执行：通过。
- 真实 Qwen 文本调用：通过。
- 真实 Qwen Tool Calling：通过，Java Tool 执行计数断言通过。
- 数据库修改：无。
- Git commit / push：未执行。
