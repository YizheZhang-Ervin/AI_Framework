package com.ervin.demo_agentscope.agent;

import io.agentscope.core.ReActAgent;
import io.agentscope.core.agent.Agent;
import io.agentscope.core.message.Msg;
import io.agentscope.core.model.OllamaChatModel;
import io.agentscope.core.tool.Toolkit;
import io.agentscope.core.tool.mcp.McpClientBuilder;
import io.agentscope.core.tool.mcp.McpClientWrapper;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class McpAgent {
    @Autowired
    OllamaChatModel model;

    Agent reactAgent;

    @PostConstruct
    public void init(){
        // 6）mcp
        // 注册 MCP 服务器的所有工具
        McpClientWrapper mcpClient = McpClientBuilder.create("filesystem-mcp")
                .stdioTransport("npx", "-y", "@modelcontextprotocol/server-filesystem", "/tmp")
                .buildAsync()
                .block();
        Toolkit toolkit = new Toolkit();
        toolkit.registerMcpClient(mcpClient).block();
        // TODO：传输配置、工具过滤、工具组、配置选项、管理客户端、Higress AI Gateway 集成

        this.reactAgent = ReActAgent.builder()
                .name("McpAgent")
                .sysPrompt("你是一个名为 McpAgent 的助手")
                .model(model)
                .toolkit(toolkit)
                .build();
    }

    public String go(String content){
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
