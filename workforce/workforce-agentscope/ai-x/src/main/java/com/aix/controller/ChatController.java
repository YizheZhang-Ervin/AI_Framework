package com.aix.controller;

import com.aix.dto.ChatRequest;
import com.aix.service.ChatService;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.UUID;

/**
 * AI-X Chat REST API 控制器
 * 提供REST API和SSE流式聊天接口
 */
@RestController
@RequestMapping("/api")
public class ChatController {

    private static final Logger log = LoggerFactory.getLogger(ChatController.class);

    private final ChatService chatService;

    public ChatController(ChatService chatService) {
        this.chatService = chatService;
    }

    /**
     * 获取所有可用技能列表
     */
    @GetMapping("/skills")
    public ResponseEntity<ObjectNode> listSkills() {
        ObjectNode response = new com.fasterxml.jackson.databind.ObjectMapper().createObjectNode();
        try {
            ArrayNode skillsArray = chatService.listSkills();
            response.set("skills", skillsArray);
            response.put("total", skillsArray.size());
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("获取技能列表失败", e);
            response.put("error", "获取技能列表失败: " + e.getMessage());
            return ResponseEntity.internalServerError().body(response);
        }
    }

    /**
     * 处理聊天请求（非流式）
     */
    @PostMapping("/chat")
    public ResponseEntity<ObjectNode> chat(@RequestBody ChatRequest request) {
        ObjectNode response = new com.fasterxml.jackson.databind.ObjectMapper().createObjectNode();
        try {
            String message = request.message();
            String sessionId = request.sessionId() != null ? request.sessionId() : UUID.randomUUID().toString();
            String userId = request.userId() != null ? request.userId() : "anonymous";

            if (message == null || message.isBlank()) {
                response.put("error", "消息不能为空");
                return ResponseEntity.badRequest().body(response);
            }

            String result = chatService.handleChat(message, sessionId, userId);
            response.put("response", result != null ? result : "");
            response.put("sessionId", sessionId);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("处理聊天请求失败", e);
            response.put("error", "处理请求失败: " + e.getMessage());
            return ResponseEntity.internalServerError().body(response);
        }
    }

    /**
     * 处理流式聊天请求（SSE）
     */
    @PostMapping(value = "/chat/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter chatStream(@RequestBody ChatRequest request) {
        String message = request.message();
        String sessionId = request.sessionId() != null ? request.sessionId() : UUID.randomUUID().toString();
        String userId = request.userId() != null ? request.userId() : "anonymous";

        final String finalSessionId = sessionId;
        final String finalUserId = userId;

        // 创建SseEmitter，超时时间设为10分钟
        SseEmitter emitter = new SseEmitter(600000L);

        // 在异步线程中执行流式处理
        new Thread(() -> {
            try {
                chatService.handleChatStream(message, finalSessionId, finalUserId,
                        (eventType, data) -> {
                            try {
                                SseEmitter.SseEventBuilder event = SseEmitter.event()
                                        .name(eventType)
                                        .data(data);
                                emitter.send(event);
                            } catch (IOException e) {
                                throw new RuntimeException(e);
                            }
                        });
                emitter.complete();
            } catch (Exception e) {
                log.error("流式处理异常", e);
                emitter.completeWithError(e);
            }
        }).start();

        return emitter;
    }

    /**
     * 处理带文件附件的流式聊天请求（SSE），通过multipart/form-data上传
     */
    @PostMapping(value = "/chat/stream/upload", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter chatStreamWithFile(@RequestParam("message") String message,
                                          @RequestParam(value = "sessionId", required = false) String sessionId,
                                          @RequestParam(value = "userId", required = false) String userId,
                                          @RequestParam(value = "file", required = false) MultipartFile[] files) {
        if (sessionId == null || sessionId.isBlank()) {
            sessionId = UUID.randomUUID().toString();
        }
        if (userId == null || userId.isBlank()) {
            userId = "anonymous";
        }

        final String finalSessionId = sessionId;
        final String finalUserId = userId;

        // 创建SseEmitter，超时时间设为10分钟
        SseEmitter emitter = new SseEmitter(600000L);

        // 在异步线程中执行流式处理
        new Thread(() -> {
            try {
                chatService.handleChatStreamWithFiles(message, finalSessionId, finalUserId,
                        files,
                        (eventType, data) -> {
                            try {
                                SseEmitter.SseEventBuilder event = SseEmitter.event()
                                        .name(eventType)
                                        .data(data);
                                emitter.send(event);
                            } catch (IOException e) {
                                throw new RuntimeException(e);
                            }
                        });
                emitter.complete();
            } catch (Exception e) {
                log.error("流式处理异常", e);
                emitter.completeWithError(e);
            }
        }).start();

        return emitter;
    }

}