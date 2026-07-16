package com.aix.init;

import com.aix.config.AppConfig;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.agentscope.core.model.Model;
import io.agentscope.core.model.ModelCreationContext;
import io.agentscope.core.model.ModelRegistry;
import io.agentscope.harness.agent.HarnessAgent;
import io.agentscope.harness.agent.memory.compaction.CompactionConfig;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.stream.Stream;

/**
 * AgentScope 智能体初始化器
 * 负责 workspace 初始化、技能加载、AgentScope 智能体创建
 * ChatService 通过 getAgent() / getWorkspacePath() 获取初始化后的实例
 */
@Component
public class AgentInitializer {

    private static final Logger log = LoggerFactory.getLogger(AgentInitializer.class);

    private final AppConfig appConfig;
    private final ObjectMapper objectMapper;

    private HarnessAgent agent;
    private Path workspacePath;

    public AgentInitializer(AppConfig appConfig, ObjectMapper objectMapper) {
        this.appConfig = appConfig;
        this.objectMapper = objectMapper;
    }

    /**
     * 初始化（Spring 启动后自动调用）
     */
    @PostConstruct
    public void init() throws Exception {
        AppConfig.ServerConfig serverConfig = appConfig.getServer();
        AppConfig.ModelConfig modelConfig = appConfig.getModel();
        AppConfig.AgentConfig agentConfig = appConfig.getAgent();

        String skillsDirName = serverConfig.getSkillsDir();
        workspacePath = Paths.get(serverConfig.getWorkspace()).toAbsolutePath().normalize();

        log.info("=".repeat(60));
        log.info("AI-X Agent 初始化中...");
        log.info("=".repeat(60));
        log.info("端口: {}", serverConfig.getPort());
        log.info("模型提供商: {}", modelConfig.getProvider());
        log.info("模型名称: {}", modelConfig.getName());
        log.info("API密钥: {}", modelConfig.getApiKey() != null && !modelConfig.getApiKey().isEmpty() ? "已设置" : "未设置");
        log.info("API基础URL: {}", modelConfig.getBaseUrl() != null && !modelConfig.getBaseUrl().isEmpty() ? modelConfig.getBaseUrl() : "使用默认地址");
        log.info("工作空间: {}", workspacePath);
        log.info("Skills目录: {}/{} (classpath)", skillsDirName, "");
        log.info("=".repeat(60));

        // 1. 初始化workspace并从classpath加载skills
        initializeWorkspace(skillsDirName);

        // 2. 创建AgentScope智能体
        createAgent(modelConfig, agentConfig);
    }

    public HarnessAgent getAgent() {
        return agent;
    }

    public Path getWorkspacePath() {
        return workspacePath;
    }

    // ===== 内部初始化方法 =====

    private void initializeWorkspace(String skillsDirName) throws IOException {
        Files.createDirectories(workspacePath);

        Path workspaceSkillsDir = workspacePath.resolve("skills");
        Files.createDirectories(workspaceSkillsDir);

        int loadedCount = 0;
        var classLoader = getClass().getClassLoader();
        InputStream indexStream = classLoader.getResourceAsStream(skillsDirName + "/skills.index");
        if (indexStream != null) {
            String indexContent = new String(indexStream.readAllBytes(), StandardCharsets.UTF_8);
            indexStream.close();
            String[] skillNames = indexContent.split("\\s+");
            for (String skillName : skillNames) {
                skillName = skillName.trim();
                if (!skillName.isEmpty()) {
                    try {
                        loadedCount += loadSkillFromClasspath(classLoader, workspaceSkillsDir, skillsDirName, skillName);
                    } catch (Exception e) {
                        log.warn("加载技能 {} 失败: {}", skillName, e.getMessage());
                    }
                }
            }
        } else {
            log.warn("未找到 skills.index 文件，无法从classpath加载技能");
        }

        log.info("从classpath加载了 {} 个技能到工作空间", loadedCount);

        try (Stream<Path> skills = Files.list(workspaceSkillsDir)) {
            List<Path> loadedSkills = skills.filter(Files::isDirectory).toList();
            if (loadedSkills.isEmpty()) {
                log.info("当前没有加载任何技能。请在 classpath:skills/ 目录下添加技能（每个技能一个子目录，包含SKILL.md文件）");
            } else {
                log.info("已加载的技能:");
                for (Path skill : loadedSkills) {
                    Path skillMd = skill.resolve("SKILL.md");
                    if (Files.exists(skillMd)) {
                        log.info("  - {} (SKILL.md 已找到)", skill.getFileName());
                    } else {
                        log.info("  - {} (SKILL.md 未找到)", skill.getFileName());
                    }
                }
            }
        }
    }

    private int loadSkillFromClasspath(ClassLoader classLoader, Path workspaceSkillsDir,
                                        String skillsDirName, String skillName) throws IOException {
        String skillPath = skillsDirName + "/" + skillName + "/SKILL.md";
        InputStream skillMdStream = classLoader.getResourceAsStream(skillPath);
        if (skillMdStream == null) {
            return 0;
        }

        Path targetSkillDir = workspaceSkillsDir.resolve(skillName);
        Files.createDirectories(targetSkillDir);

        Path targetSkillMd = targetSkillDir.resolve("SKILL.md");
        if (!Files.exists(targetSkillMd)) {
            Files.copy(skillMdStream, targetSkillMd);
            log.info("已加载技能: {} (从classpath: {})", skillName, skillPath);
        }
        skillMdStream.close();

        loadSkillSubDirFromClasspath(classLoader, workspaceSkillsDir, skillsDirName, skillName, "references");
        loadSkillSubDirFromClasspath(classLoader, workspaceSkillsDir, skillsDirName, skillName, "scripts");

        return 1;
    }

    private void loadSkillSubDirFromClasspath(ClassLoader classLoader, Path workspaceSkillsDir,
                                              String skillsDirName, String skillName, String subDir) {
        // 对于子目录，通过命名约定尝试加载已知文件
        // 因为classpath无法列出目录，所以这里只做尝试性加载
    }

    private void createAgent(AppConfig.ModelConfig modelConfig, AppConfig.AgentConfig agentConfig) {
        log.info("正在创建AgentScope智能体...");
        log.info("  模型: {}:{}", modelConfig.getProvider(), modelConfig.getName());
        log.info("  API密钥: {}", modelConfig.getApiKey() != null && !modelConfig.getApiKey().isEmpty() ? "已设置" : "未设置（使用环境变量）");
        log.info("  API基础URL: {}", modelConfig.getBaseUrl() != null && !modelConfig.getBaseUrl().isEmpty() ? modelConfig.getBaseUrl() : "使用默认地址");
        log.info("  工作空间: {}", workspacePath);

        Path agentsMdPath = workspacePath.resolve("AGENTS.md");
        if (!Files.exists(agentsMdPath)) {
            try {
                Files.writeString(agentsMdPath, String.format("""
                        # %s
                        
                        %s
                        """, agentConfig.getName(), agentConfig.getSysPrompt()), StandardCharsets.UTF_8);
                log.info("已创建默认AGENTS.md文件");
            } catch (IOException e) {
                log.warn("无法创建AGENTS.md文件: {}", e.getMessage());
            }
        }

        String modelId = modelConfig.getProvider() + ":" + modelConfig.getName();
        log.info("模型ID: {}", modelId);

        var contextBuilder = ModelCreationContext.builder();
        if (modelConfig.getApiKey() != null && !modelConfig.getApiKey().isEmpty()) {
            contextBuilder.apiKey(modelConfig.getApiKey());
            log.info("使用配置文件中的API密钥");
        }
        if (modelConfig.getBaseUrl() != null && !modelConfig.getBaseUrl().isEmpty()) {
            contextBuilder.baseUrl(modelConfig.getBaseUrl());
            log.info("使用配置文件中的API基础URL: {}", modelConfig.getBaseUrl());
        }
        ModelCreationContext modelContext = contextBuilder.build();

        Model model = ModelRegistry.resolve(modelId, modelContext);
        log.info("模型创建成功: {} (type={})", modelId, model.getClass().getSimpleName());

        agent = HarnessAgent.builder()
                .name(agentConfig.getName())
                .sysPrompt(agentConfig.getSysPrompt())
                .model(model)
                .workspace(workspacePath)
                .compaction(CompactionConfig.builder()
                        .triggerMessages(50)
                        .keepMessages(20)
                        .build())
                .enablePendingToolRecovery(true)
                .build();

        log.info("AgentScope智能体创建完成 (name={}, model={})", agentConfig.getName(), modelId);
    }
}