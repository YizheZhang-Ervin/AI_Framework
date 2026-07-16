package com.aix;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

/**
 * AI-X 智能体应用入口（Spring Boot版）
 * 基于AgentScope Java 2.0框架，自动加载skills目录下的技能
 * 配置通过 application.yaml 文件加载，使用 @ConfigurationProperties 绑定
 */
@SpringBootApplication
@EnableConfigurationProperties
public class AiXApplication {

    private static final Logger log = LoggerFactory.getLogger(AiXApplication.class);

    public static void main(String[] args) {
        log.info("=".repeat(60));
        log.info("AI-X Agent (Spring Boot版) 启动中...");
        log.info("=".repeat(60));
        SpringApplication.run(AiXApplication.class, args);
    }
}