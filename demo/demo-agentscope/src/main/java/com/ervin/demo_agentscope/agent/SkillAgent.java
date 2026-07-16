package com.ervin.demo_agentscope.agent;

import com.ervin.demo_agentscope.skill.SimpleSkill;
import io.agentscope.core.ReActAgent;
import io.agentscope.core.agent.Agent;
import io.agentscope.core.message.Msg;
import io.agentscope.core.model.OllamaChatModel;
import io.agentscope.core.skill.SkillBox;
import io.agentscope.core.tool.Toolkit;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class SkillAgent {
    @Autowired
    OllamaChatModel model;

    Agent reactAgent;

    @PostConstruct
    public void init() {
        // 7）skill
        Toolkit toolkit = new Toolkit();
        SkillBox skillBox = new SkillBox(toolkit);
        skillBox.registerSkill(SimpleSkill.dataAnalysis1());
        // TODO：渐进式披露、代码执行能力、持久化存储、自定义提示词

        this.reactAgent = ReActAgent.builder()
                .name("SkillAgent")
                .sysPrompt("你是一个名为 SkillAgent 的助手")
                .model(model)
                .toolkit(toolkit)
                .skillBox(skillBox)
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
