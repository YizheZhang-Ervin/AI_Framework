package com.ervin.demo_agentscope.agent;

import io.agentscope.core.ReActAgent;
import io.agentscope.core.agent.Agent;
import io.agentscope.core.memory.InMemoryMemory;
import io.agentscope.core.memory.LongTermMemoryMode;
import io.agentscope.core.memory.mem0.Mem0LongTermMemory;
import io.agentscope.core.message.Msg;
import io.agentscope.core.model.OllamaChatModel;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class MemoryAgent {
    @Autowired
    OllamaChatModel model;

    Agent reactAgent;

    @PostConstruct
    public void init() {
        // 11）记忆
        // 使用 Platform Mem0（默认，无需指定 apiType）
        Mem0LongTermMemory longTermMemory = Mem0LongTermMemory.builder()
                .agentName("SmartAssistant")
                .userId("user-001")
                .apiBaseUrl("https://api.mem0.ai")
                .apiKey(System.getenv("MEM0_API_KEY"))
                .build();
        // TODO:AutoContextMemory,短期记忆持久化,ReMeLongTermMemory,BailianLongTermMemory

        // 13）状态
        // TODO:session，sessionKey，自定义组件

        // 14）会话
        // TODO:jsonSession，inMemorySession，redis/mysql的session

        this.reactAgent = ReActAgent.builder()
                .name("MemoryAgent")
                .sysPrompt("你是一个名为 MemoryAgent 的助手")
                .model(model)
                // 短期记忆
                .memory(new InMemoryMemory())
                // 长期记忆
                .longTermMemory(longTermMemory)
                .longTermMemoryMode(LongTermMemoryMode.STATIC_CONTROL)
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
