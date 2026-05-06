package com.ervin.demo_agentscope.config;

import io.agentscope.core.formatter.dashscope.DashScopeChatFormatter;
import io.agentscope.core.model.DashScopeChatModel;
import io.agentscope.core.model.OllamaChatModel;
import io.agentscope.core.model.ollama.OllamaOptions;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class ModelConfig {

    String apiKey = System.getenv("DASHSCOPE_API_KEY");

    @Bean
    public OllamaChatModel ollamaChatModel(){
        OllamaOptions options = OllamaOptions.builder()
                .numCtx(4096)           // 上下文窗口大小
                .temperature(0.7)       // 生成随机性
                .topK(40)               // Top-K 采样
                .topP(0.9)              // 核采样
                .repeatPenalty(1.1)     // 重复惩罚
                .build();
        return OllamaChatModel.builder()
                .modelName("qwen3-0.6b")
                .baseUrl("http://localhost:11434")
                .defaultOptions(options)  // 内部转换为 OllamaOptions
                .build();
    }

    @Bean
    public DashScopeChatModel dashScopeChatModel(){
        return DashScopeChatModel.builder()
                .apiKey(apiKey)
                .modelName("qwen-vl-max")
                .stream(true)
                .formatter(new DashScopeChatFormatter())
                .build();
    }
}
