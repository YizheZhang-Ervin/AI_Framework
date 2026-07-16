package com.ervin.demo_agentscope.agent;

import io.agentscope.core.ReActAgent;
import io.agentscope.core.agent.Agent;
import io.agentscope.core.message.Msg;
import io.agentscope.core.model.OllamaChatModel;
import io.agentscope.core.model.StructuredOutputReminder;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class StructuredOutputAgent {
    @Autowired
    OllamaChatModel model;

    Agent reactAgent;

    @PostConstruct
    public void init() {
        // 16）结构化输出
        // TODO: 嵌套结构、jackson注解

        this.reactAgent = ReActAgent.builder()
                .name("StructuredOutputAgent")
                .sysPrompt("你是一个名为 StructuredOutputAgent 的助手")
                .model(model)
                // 结构化输出
                .structuredOutputReminder(StructuredOutputReminder.TOOL_CHOICE)  // 或 PROMPT
                .build();
    }

    public Object go(Msg userMsg,Class<?> structuredModel) {
        try {
            // 调用 ReAct Agent
            Msg response = reactAgent.call(userMsg,structuredModel).block();
            if (response != null && response.getStructuredData(structuredModel) != null) {
                return response.getStructuredData(structuredModel);
            } else {
                return "Failed: please try again";
            }
        } catch (Exception e) {
            return "Failed：" + e.getMessage();
        }
    }
}
