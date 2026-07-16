package com.ervin.demo_agentscope.agent;

import com.ervin.demo_agentscope.hook.ConfirmHook;
import com.ervin.demo_agentscope.hook.LoggingHook;
import io.agentscope.core.ReActAgent;
import io.agentscope.core.agent.Agent;
import io.agentscope.core.message.Msg;
import io.agentscope.core.model.OllamaChatModel;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class HookAgent {
    @Autowired
    OllamaChatModel model;

    Agent reactAgent;

    @PostConstruct
    public void init() {
        // 9）hook
        // TODO：带优先级hook、修改事件、监控工具执行、监控错误、内置的 JSONL 导出器

        // 10）HITL
        // TODO：处理暂停和恢复

        this.reactAgent = ReActAgent.builder()
                .name("HookAgent")
                .sysPrompt("你是一个名为 HookAgent 的助手")
                .model(model)
                // hooks
                .hooks(List.of(
                        new LoggingHook(),
                        new ConfirmHook()  // HITL
                ))
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
