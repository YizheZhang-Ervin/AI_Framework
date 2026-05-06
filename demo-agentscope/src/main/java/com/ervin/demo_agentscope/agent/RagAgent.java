package com.ervin.demo_agentscope.agent;

import com.ervin.demo_agentscope.service.RagService;
import io.agentscope.core.ReActAgent;
import io.agentscope.core.agent.Agent;
import io.agentscope.core.message.Msg;
import io.agentscope.core.model.OllamaChatModel;
import io.agentscope.core.rag.RAGMode;
import io.agentscope.core.rag.model.RetrieveConfig;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class RagAgent {
    @Autowired
    OllamaChatModel model;
    @Autowired
    RagService ragService;

    Agent reactAgent;

    @PostConstruct
    public void init(){
        // 8) rag
        // TODO：云托管知识库bailian、dify知识库、ragflow知识库、自定义rag

        this.reactAgent = ReActAgent.builder()
                .name("RagAgent")
                .sysPrompt("你是一个名为 RagAgent 的助手")
                .model(model)
                // 启用 Generic RAG 模式
//                .knowledge(knowledge)
//                .ragMode(RAGMode.GENERIC)
//                .retrieveConfig(
//                        RetrieveConfig.builder()
//                                .limit(3)
//                                .scoreThreshold(0.3)
//                                .build())
                // 启用 Agentic RAG 模式
                .knowledge(ragService.getKnowledge())
                .ragMode(RAGMode.AGENTIC)
                .retrieveConfig(
                        RetrieveConfig.builder()
                                .limit(3)
                                .scoreThreshold(0.5)
                                .build())
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
