package com.aix.dto;

/**
 * 聊天请求体
 */
public record ChatRequest(String message, String sessionId, String userId) {
}