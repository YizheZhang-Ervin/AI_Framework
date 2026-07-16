package com.ervin.demo_agentscope.agent;

import com.ervin.demo_agentscope.model.UserContext;
import com.ervin.demo_agentscope.tool.SimpleTools;
import io.agentscope.core.ReActAgent;
import io.agentscope.core.agent.Agent;
import io.agentscope.core.message.Msg;
import io.agentscope.core.model.ExecutionConfig;
import io.agentscope.core.model.OllamaChatModel;
import io.agentscope.core.model.ToolSchema;
import io.agentscope.core.tool.ToolExecutionContext;
import io.agentscope.core.tool.Toolkit;
import io.agentscope.core.tool.ToolkitConfig;
import io.agentscope.core.tool.file.ReadFileTool;
import io.agentscope.core.tool.file.WriteFileTool;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.List;
import java.util.Map;

@Component
public class ToolAgent {
    @Autowired
    OllamaChatModel model;
    @Autowired
    SimpleTools simpleTools;

    Agent reactAgent;

    @PostConstruct
    public void init() {
        // 1）工具
        //Toolkit toolkit = new Toolkit();
        Toolkit toolkit = new Toolkit(ToolkitConfig.builder()
                .parallel(true)                    // 并行执行多个工具
                .allowToolDeletion(false)          // 禁止删除工具
                .executionConfig(ExecutionConfig.builder()
                        .timeout(Duration.ofSeconds(30))
                        .build())
                .build());
        // 方法1:常规注册方式
        // toolkit.registerTool(simpleTools);
        // 方法2:创建工具组
        toolkit.createToolGroup("simple", "基础工具", true);   // 默认激活
        // 注册到工具组
        toolkit.registration()
                .tool(simpleTools)
                .group("simple")
                // 预设参数
                .presetParameters(Map.of(
                        "send", Map.of("apiKey", System.getenv("EMAIL_API_KEY"))
                ))
                .apply();
        // 动态切换工具组
        // toolkit.updateToolGroups(List.of("simple"), true);   // 激活

        // 2）工具上下文
        // 注册到 Agent
        ToolExecutionContext context = ToolExecutionContext.builder()
                .register(new UserContext("user-123"))
                .build();

        // 3）内置工具
        // 基础注册
        toolkit.registerTool(new ReadFileTool());
        toolkit.registerTool(new WriteFileTool());
        // 安全模式（推荐）：限制文件访问范围
        //toolkit.registerTool(new ReadFileTool("/safe/workspace"));
        //toolkit.registerTool(new WriteFileTool("/safe/workspace"));
        // shell
        //Function<String, Boolean> callback = cmd -> askUserForApproval(cmd);
        //toolkit.registerTool(new ShellCommandTool(allowedCommands, callback));
        // 多模态
        //toolkit.registerTool(new DashScopeMultiModalTool(System.getenv("DASHSCOPE_API_KEY")));
        //toolkit.registerTool(new OpenAIMultiModalTool(System.getenv("OPENAI_API_KEY")));

        // 4）智能体根据任务需求自主选择激活哪些工具组
        // toolkit.registerMetaTool();

        // 5）schema工具
        // 工具由外部系统实现（如前端、其他服务）or 动态注册第三方工具
        // 方式一：使用 ToolSchema
        ToolSchema schema = ToolSchema.builder()
                .name("query_database")
                .description("查询外部数据库")
                .parameters(Map.of(
                        "type", "object",
                        "properties", Map.of("sql", Map.of("type", "string")),
                        "required", List.of("sql")
                ))
                .build();
        toolkit.registerSchema(schema);
        // 方式二：批量注册
        //toolkit.registerSchemas(List.of(schema1, schema2));
        // 检查是否为外部工具
        //boolean isExternal = toolkit.isExternalTool("query_database");  // true

        this.reactAgent = ReActAgent.builder()
                .name("ToolAgent")
                .sysPrompt("你是一个名为 ToolAgent 的助手")
                .model(model)
                .toolkit(toolkit)
                .toolExecutionContext(context)
                .build();
    }

    public String go(String content) {
        try {
            Msg userMsg = Msg.builder().textContent(content).build();
            // 调用 ReAct Agent
            Msg response = reactAgent.call(userMsg).block();
            if (response != null && response.getTextContent() != null) {
                return response.getTextContent();
            } else {
                return "Failed: please try again";
            }
        } catch (Exception e) {
            return "Failed：" + e.getMessage();
        }
    }
}
