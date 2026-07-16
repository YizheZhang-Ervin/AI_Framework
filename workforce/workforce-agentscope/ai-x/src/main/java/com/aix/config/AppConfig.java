package com.aix.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * AI-X 应用配置
 * 使用 Spring Boot @ConfigurationProperties 绑定 application.yaml 中 aix.* 下的配置
 */
@Configuration
@ConfigurationProperties(prefix = "aix")
public class AppConfig {

    private ModelConfig model = new ModelConfig();
    private ServerConfig server = new ServerConfig();
    private AgentConfig agent = new AgentConfig();

    public ModelConfig getModel() { return model; }
    public void setModel(ModelConfig model) { this.model = model; }

    public ServerConfig getServer() { return server; }
    public void setServer(ServerConfig server) { this.server = server; }

    public AgentConfig getAgent() { return agent; }
    public void setAgent(AgentConfig agent) { this.agent = agent; }

    // ===== 内部配置类 =====

    public static class ModelConfig {
        private String provider = "openai";
        private String name = "gpt-4o";
        private String apiKey = "";
        private String baseUrl = "";
        private String organizationId = "";

        public String getProvider() { return provider; }
        public void setProvider(String provider) { this.provider = provider; }

        public String getName() { return name; }
        public void setName(String name) { this.name = name; }

        public String getApiKey() { return apiKey; }
        public void setApiKey(String apiKey) { this.apiKey = apiKey; }

        public String getBaseUrl() { return baseUrl; }
        public void setBaseUrl(String baseUrl) { this.baseUrl = baseUrl; }

        public String getOrganizationId() { return organizationId; }
        public void setOrganizationId(String organizationId) { this.organizationId = organizationId; }

        /**
         * 获取AgentScope的模型ID字符串（provider:name格式）
         */
        public String getModelId() {
            return provider + ":" + name;
        }
    }

    public static class ServerConfig {
        private int port = 8080;
        private String workspace = ".agentscope/workspace";
        private String skillsDir = "skills";

        public int getPort() { return port; }
        public void setPort(int port) { this.port = port; }

        public String getWorkspace() { return workspace; }
        public void setWorkspace(String workspace) { this.workspace = workspace; }

        public String getSkillsDir() { return skillsDir; }
        public void setSkillsDir(String skillsDir) { this.skillsDir = skillsDir; }
    }

    public static class AgentConfig {
        private String name = "ai-x-agent";
        private String sysPrompt = "You are a helpful AI assistant.";
        private int maxIters = 10;

        public String getName() { return name; }
        public void setName(String name) { this.name = name; }

        public String getSysPrompt() { return sysPrompt; }
        public void setSysPrompt(String sysPrompt) { this.sysPrompt = sysPrompt; }

        public int getMaxIters() { return maxIters; }
        public void setMaxIters(int maxIters) { this.maxIters = maxIters; }
    }
}