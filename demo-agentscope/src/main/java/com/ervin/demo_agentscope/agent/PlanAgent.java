package com.ervin.demo_agentscope.agent;

import io.agentscope.core.ReActAgent;
import io.agentscope.core.agent.Agent;
import io.agentscope.core.message.Msg;
import io.agentscope.core.model.OllamaChatModel;
import io.agentscope.core.plan.PlanNotebook;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class PlanAgent {
    @Autowired
    OllamaChatModel model;

    Agent reactAgent;

    @PostConstruct
    public void init() {
        // 12) 计划
        PlanNotebook planNotebook = PlanNotebook.builder()
                .maxSubtasks(10)  // 限制子任务数量
                .build();
        // TODO:工具、流程、配置（用户确认（needUserConfirm），限制子任务数量，自定义存储，自定义提示生成）

        this.reactAgent = ReActAgent.builder()
                .name("PlanAgent")
                .sysPrompt("你是一个名为 PlanAgent 的助手")
                .model(model)
                // plan
                // .enablePlan()
                .planNotebook(planNotebook)
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
