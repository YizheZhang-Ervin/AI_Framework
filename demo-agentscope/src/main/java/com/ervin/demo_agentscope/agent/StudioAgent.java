package com.ervin.demo_agentscope.agent;

import com.ervin.demo_agentscope.hook.ConfirmHook;
import com.ervin.demo_agentscope.hook.LoggingHook;
import io.agentscope.core.ReActAgent;
import io.agentscope.core.agent.Agent;
import io.agentscope.core.message.Msg;
import io.agentscope.core.model.OllamaChatModel;
import io.agentscope.core.studio.StudioManager;
import io.agentscope.core.studio.StudioMessageHook;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class StudioAgent {

    @Autowired
    OllamaChatModel model;

    Agent reactAgent;

    @PostConstruct
    public void init() {
        // studio
        // TODO: studioUserAgent、手动推送消息、多agent可视化、OpenTelemetry 链路追踪(langfuse/其他)

        this.reactAgent = ReActAgent.builder()
                .name("StudioAgent")
                .sysPrompt("你是一个名为 StudioAgent 的助手")
                .model(model)
                .hook(new StudioMessageHook(StudioManager.getClient()))
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
