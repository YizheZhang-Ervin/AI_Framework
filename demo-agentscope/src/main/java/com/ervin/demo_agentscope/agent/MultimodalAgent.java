package com.ervin.demo_agentscope.agent;

import io.agentscope.core.ReActAgent;
import io.agentscope.core.agent.Agent;
import io.agentscope.core.memory.InMemoryMemory;
import io.agentscope.core.message.*;
import io.agentscope.core.model.DashScopeChatModel;
import io.agentscope.core.tool.Toolkit;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class MultimodalAgent {
    // 15) 多模态
    // TODO:contentBlock结构

    @Autowired
    DashScopeChatModel dashScopeChatModel;

    Agent reactAgent;

    @PostConstruct
    public void init() {
        this.reactAgent = ReActAgent.builder()
                .name("MultimodalAgent")
                .sysPrompt("你是一个具有视觉能力的 AI 助手")
                .model(dashScopeChatModel)
                .memory(new InMemoryMemory())
                .toolkit(new Toolkit())
                .build();
    }

    public String go(String base64Image) {
        try {
            // 2. 创建多模态消息
            // String base64Image = "iVBORw0KGgoAAAANSUhEUgAAABQAAAAUCAIAAAAC64pa...";
            Msg userMsg = Msg.builder().role(MsgRole.USER).content(List.of(TextBlock.builder().text("这张图片是什么颜色？").build(), ImageBlock.builder().source(Base64Source.builder().data(base64Image).mediaType("image/png").build()).build())).build();
            // 3. 发送请求并获取响应
            Msg response = this.reactAgent.call(userMsg).block();
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
