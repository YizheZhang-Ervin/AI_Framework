package com.aix.service;

import com.aix.init.AgentInitializer;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import io.agentscope.core.agent.RuntimeContext;
import io.agentscope.core.event.AgentEventType;
import io.agentscope.core.event.TextBlockDeltaEvent;
import io.agentscope.core.event.ThinkingBlockDeltaEvent;
import io.agentscope.core.event.ToolCallStartEvent;
import io.agentscope.core.event.ToolResultTextDeltaEvent;
import io.agentscope.core.message.UserMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Duration;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.BiConsumer;
import java.util.stream.Stream;

/**
 * AI-X 聊天服务
 * 负责会话管理和与 Agent 的交互
 * 初始化工作委托给 AgentInitializer 完成
 */
@Service
public class ChatService {

    private static final Logger log = LoggerFactory.getLogger(ChatService.class);

    private final AgentInitializer agentInitializer;
    private final ObjectMapper objectMapper;

    // 会话ID管理
    private final ConcurrentHashMap<String, String> sessionStore = new ConcurrentHashMap<>();

    public ChatService(AgentInitializer agentInitializer, ObjectMapper objectMapper) {
        this.agentInitializer = agentInitializer;
        this.objectMapper = objectMapper;
    }

    /**
     * 获取技能列表（JSON树）
     */
    public ArrayNode listSkills() throws IOException {
        Path skillsDir = agentInitializer.getWorkspacePath().resolve("skills");
        ArrayNode skillsArray = objectMapper.createArrayNode();

        if (Files.exists(skillsDir) && Files.isDirectory(skillsDir)) {
            try (Stream<Path> skillDirs = Files.list(skillsDir)) {
                List<Path> skillList = skillDirs
                        .filter(Files::isDirectory)
                        .sorted()
                        .toList();

                for (Path skillDir : skillList) {
                    Path skillMdPath = skillDir.resolve("SKILL.md");
                    if (Files.exists(skillMdPath)) {
                        String content = Files.readString(skillMdPath, StandardCharsets.UTF_8);
                        ObjectNode skillNode = objectMapper.createObjectNode();
                        skillNode.put("name", skillDir.getFileName().toString());
                        skillNode.put("path", skillDir.toString());

                        skillNode.put("description", parseDescription(content));

                        ArrayNode files = objectMapper.createArrayNode();
                        try (Stream<Path> skillFiles = Files.list(skillDir)) {
                            skillFiles
                                    .filter(p -> !p.getFileName().toString().startsWith("."))
                                    .forEach(p -> files.add(p.getFileName().toString()));
                        }
                        skillNode.set("files", files);

                        skillsArray.add(skillNode);
                    }
                }
            }
        }

        return skillsArray;
    }

    /**
     * 处理聊天请求（非流式）
     */
    public String handleChat(String message, String sessionId, String userId) {
        if (sessionId == null || sessionId.isBlank()) {
            sessionId = UUID.randomUUID().toString();
        }
        if (userId == null || userId.isBlank()) {
            userId = "anonymous";
        }
        sessionStore.put(userId, sessionId);

        log.info("收到消息 (sessionId={}, userId={}): {}", sessionId, userId, message);

        RuntimeContext ctx = RuntimeContext.builder()
                .sessionId(sessionId)
                .userId(userId)
                .build();

        String response = agentInitializer.getAgent().call(new UserMessage(message), ctx)
                .timeout(Duration.ofMinutes(5))
                .block()
                .getTextContent();

        log.info("智能体回复 (sessionId={}): {}", sessionId,
                response != null && response.length() > 100 ? response.substring(0, 100) + "..." : response);

        return response != null ? response : "";
    }

    /**
     * 处理流式聊天请求，通过回调逐块发送SSE数据
     */
    public void handleChatStream(String message, String sessionId, String userId,
                                  BiConsumer<String, String> eventCallback) {
        handleChatStreamWithFiles(message, sessionId, userId, null, eventCallback);
    }

    /**
     * 处理带多个文件附件的流式聊天请求，通过回调逐块发送SSE数据
     * 策略：所有附件一律保存到工作空间，永不内联文件内容到消息中。
     * 消息只包含文件路径和基本信息，AI 通过 read_file 工具按需读取。
     * 这样确保会话历史永远保持小体积，避免 ContextWindowExceededError。
     */
    public void handleChatStreamWithFiles(String message, String sessionId, String userId,
                                          MultipartFile[] files,
                                          BiConsumer<String, String> eventCallback) {
        if (sessionId == null || sessionId.isBlank()) {
            sessionId = UUID.randomUUID().toString();
        }
        if (userId == null || userId.isBlank()) {
            userId = "anonymous";
        }
        sessionStore.put(userId, sessionId);

        log.info("收到流式聊天请求 (sessionId={}, userId={}): {}", sessionId, userId, message);

        StringBuilder enrichedMessage = new StringBuilder(message);

        Path uploadDir = agentInitializer.getWorkspacePath().resolve("uploads").resolve(sessionId);

        if (files != null && files.length > 0) {
            enrichedMessage.append("\n\n【用户上传了以下文件，请使用 read_file 工具按需读取】");
            for (MultipartFile file : files) {
                if (file != null && !file.isEmpty()) {
                    try {
                        String fileName = file.getOriginalFilename();
                        if (fileName == null || fileName.isBlank()) {
                            fileName = "unnamed_file";
                        }
                        fileName = Paths.get(fileName).getFileName().toString();
                        byte[] fileContent = file.getBytes();
                        String fileType = detectFileType(fileName);
                        long fileSize = fileContent.length;

                        Files.createDirectories(uploadDir);
                        String safeFileName = UUID.randomUUID().toString().substring(0, 8) + "_" + fileName;
                        Path filePath = uploadDir.resolve(safeFileName);
                        Files.write(filePath, fileContent);

                        enrichedMessage.append("\n- ").append(fileName)
                                .append(" (").append(formatFileSize(fileSize)).append(", ").append(fileType).append(")")
                                .append("\n  路径: uploads/").append(sessionId).append("/").append(safeFileName);

                        log.info("文件附件保存到工作空间: {} -> uploads/{}/{} ({} bytes)", fileName, sessionId, safeFileName, fileSize);
                    } catch (IOException e) {
                        log.warn("保存文件附件失败: {}", e.getMessage());
                    }
                }
            }
            enrichedMessage.append("\n【用户上传文件结束，请使用 read_file 工具读取所需文件】");
            log.info("收到带附件的流式聊天请求 (sessionId={}, userId={}, files={})", sessionId, userId, files.length);
        }

        String finalMessage = enrichedMessage.toString();

        RuntimeContext ctx = RuntimeContext.builder()
                .sessionId(sessionId)
                .userId(userId)
                .build();

        UserMessage userMsg = new UserMessage(finalMessage);

        try {
            eventCallback.accept("session", "{\"sessionId\":\"" + sessionId + "\"}");
            eventCallback.accept("start", "{\"message\":\"开始处理\"}");

            agentInitializer.getAgent().streamEvents(userMsg, ctx)
                    .doOnNext(event -> {
                        try {
                            if (event.getType() == AgentEventType.TEXT_BLOCK_DELTA) {
                                String delta = ((TextBlockDeltaEvent) event).getDelta();
                                if (delta != null && !delta.isEmpty()) {
                                    String escapedDelta = escapeJson(delta);
                                    eventCallback.accept("delta", "{\"text\":\"" + escapedDelta + "\"}");
                                }
                            } else if (event.getType() == AgentEventType.THINKING_BLOCK_START) {
                                eventCallback.accept("thinking_start", "{}");
                            } else if (event.getType() == AgentEventType.THINKING_BLOCK_DELTA) {
                                String delta = ((ThinkingBlockDeltaEvent) event).getDelta();
                                if (delta != null && !delta.isEmpty()) {
                                    String escapedDelta = escapeJson(delta);
                                    eventCallback.accept("thinking_delta", "{\"text\":\"" + escapedDelta + "\"}");
                                }
                            } else if (event.getType() == AgentEventType.THINKING_BLOCK_END) {
                                eventCallback.accept("thinking_end", "{}");
                            } else if (event.getType() == AgentEventType.TOOL_CALL_START) {
                                String toolName = ((ToolCallStartEvent) event).getToolCallName();
                                eventCallback.accept("tool_start", "{\"tool\":\"" + toolName + "\"}");
                            } else if (event.getType() == AgentEventType.TOOL_RESULT_TEXT_DELTA) {
                                String delta = ((ToolResultTextDeltaEvent) event).getDelta();
                                if (delta != null && !delta.isEmpty()) {
                                    String escapedDelta = escapeJson(delta);
                                    eventCallback.accept("tool_delta", "{\"text\":\"" + escapedDelta + "\"}");
                                }
                            }
                        } catch (Exception e) {
                            log.warn("写入SSE事件失败", e);
                            throw new RuntimeException(e);
                        }
                    })
                    .blockLast();

            eventCallback.accept("done", "{}");

        } catch (Exception e) {
            log.error("流式处理失败", e);
            try {
                ObjectNode errorNode = objectMapper.createObjectNode();
                errorNode.put("error", e.getMessage() != null ? e.getMessage() : "未知错误");
                eventCallback.accept("error", objectMapper.writeValueAsString(errorNode));
            } catch (Exception jsonError) {
                eventCallback.accept("error", "{\"error\":\"处理过程发生错误\"}");
            }
        }
    }

    /**
     * 转义字符串为JSON字符串值
     */
    private static String escapeJson(String text) {
        return text
                .replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n")
                .replace("\r", "\\r")
                .replace("\t", "\\t");
    }

    /**
     * 根据文件名检测文件类型（MIME类型）
     */
    private static String detectFileType(String fileName) {
        if (fileName == null) return "application/octet-stream";
        String lower = fileName.toLowerCase();
        if (lower.endsWith(".txt")) return "text/plain";
        if (lower.endsWith(".md") || lower.endsWith(".markdown")) return "text/markdown";
        if (lower.endsWith(".json")) return "application/json";
        if (lower.endsWith(".xml")) return "application/xml";
        if (lower.endsWith(".html") || lower.endsWith(".htm")) return "text/html";
        if (lower.endsWith(".css")) return "text/css";
        if (lower.endsWith(".js")) return "application/javascript";
        if (lower.endsWith(".py")) return "text/x-python";
        if (lower.endsWith(".java")) return "text/x-java";
        if (lower.endsWith(".yaml") || lower.endsWith(".yml")) return "text/yaml";
        if (lower.endsWith(".csv")) return "text/csv";
        if (lower.endsWith(".sql")) return "text/sql";
        if (lower.endsWith(".sh")) return "text/x-shellscript";
        if (lower.endsWith(".properties")) return "text/x-java-properties";
        if (lower.endsWith(".pdf")) return "application/pdf";
        if (lower.endsWith(".png")) return "image/png";
        if (lower.endsWith(".jpg") || lower.endsWith(".jpeg")) return "image/jpeg";
        if (lower.endsWith(".gif")) return "image/gif";
        if (lower.endsWith(".svg")) return "image/svg+xml";
        if (lower.endsWith(".zip")) return "application/zip";
        if (lower.endsWith(".tar.gz") || lower.endsWith(".tgz")) return "application/gzip";
        if (lower.endsWith(".log")) return "text/plain";
        return "application/octet-stream";
    }

    /**
     * 判断是否为文本文件（基于MIME类型）
     */
    private static boolean isTextFile(String mimeType) {
        return mimeType != null && (mimeType.startsWith("text/") ||
                mimeType.equals("application/json") ||
                mimeType.equals("application/xml") ||
                mimeType.equals("application/javascript") ||
                mimeType.equals("text/yaml") ||
                mimeType.equals("text/x-python") ||
                mimeType.equals("text/x-java") ||
                mimeType.equals("text/csv") ||
                mimeType.equals("text/sql") ||
                mimeType.equals("text/x-shellscript") ||
                mimeType.equals("text/x-java-properties") ||
                mimeType.equals("text/markdown") ||
                mimeType.equals("image/svg+xml"));
    }

    /**
     * 格式化文件大小为人类可读格式
     */
    private static String formatFileSize(long bytes) {
        if (bytes < 1024) return bytes + " B";
        if (bytes < 1024 * 1024) return String.format("%.1f KB", bytes / 1024.0);
        if (bytes < 1024 * 1024 * 1024) return String.format("%.1f MB", bytes / (1024.0 * 1024.0));
        return String.format("%.1f GB", bytes / (1024.0 * 1024.0 * 1024.0));
    }

    /**
     * 从SKILL.md内容中解析简短的描述
     */
    private static String parseDescription(String content) {
        if (content == null || content.isBlank()) {
            return "无描述";
        }
        try {
            String[] lines = content.split("\n");
            boolean inFrontmatter = false;
            for (int i = 0; i < lines.length; i++) {
                String line = lines[i].trim();
                if (line.equals("---")) {
                    if (!inFrontmatter) {
                        inFrontmatter = true;
                        continue;
                    } else {
                        break;
                    }
                }
                if (inFrontmatter && line.startsWith("description:")) {
                    String desc = line.substring("description:".length()).trim();
                    if (desc.startsWith("\"") && desc.endsWith("\"")) {
                        desc = desc.substring(1, desc.length() - 1);
                    }
                    if (!desc.isEmpty()) {
                        return desc;
                    }
                }
            }
        } catch (Exception e) {
            // fallback
        }
        return "无描述";
    }
}