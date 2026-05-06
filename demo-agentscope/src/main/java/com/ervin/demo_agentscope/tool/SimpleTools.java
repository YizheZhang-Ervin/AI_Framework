package com.ervin.demo_agentscope.tool;

import com.ervin.demo_agentscope.model.UserContext;
import io.agentscope.core.message.ToolResultBlock;
import io.agentscope.core.tool.Tool;
import io.agentscope.core.tool.ToolEmitter;
import io.agentscope.core.tool.ToolParam;
import io.agentscope.core.tool.ToolSuspendException;
import org.springframework.stereotype.Component;

@Component
public class SimpleTools {
    @Tool(name = "get_time", description = "获取当前时间")
    public String getTime(
            @ToolParam(name = "zone", description = "时区，例如：北京") String zone) {
        return java.time.LocalDateTime.now()
                .format(java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
    }

    @Tool(description = "获取指定城市的天气")
    public String getWeather(
            @ToolParam(name = "city", description = "城市名称") String city) {
        return city + " 的天气：晴天，25°C";
    }

    // 同步工具
    @Tool(description = "计算两数之和")
    public int add(
            @ToolParam(name = "a", description = "第一个数") int a,
            @ToolParam(name = "b", description = "第二个数") int b) {
        return a + b;
    }

    // 异步工具
//    @Tool(description = "异步搜索")
//    public Mono<String> search(
//            @ToolParam(name = "query", description = "搜索词") String query) {
//        return webClient.get()
//                .uri("/search?q=" + query)
//                .retrieve()
//                .bodyToMono(String.class);
//    }

    // 流式工具
    @Tool(description = "生成数据")
    public ToolResultBlock generate(
            @ToolParam(name = "count") int count,
            ToolEmitter emitter) {  // 自动注入，无需 @ToolParam
        for (int i = 0; i < count; i++) {
            emitter.emit(ToolResultBlock.text("进度 " + i));
        }
        return ToolResultBlock.text("完成");
    }

    // 预设参数
    @Tool(description = "发送邮件")
    public String send(
            @ToolParam(name = "to") String to,
            @ToolParam(name = "subject") String subject,
            @ToolParam(name = "apiKey") String apiKey) {  // 预设，LLM 不可见
        return "已发送";
    }

    // 工具执行上下文
    @Tool(description = "查询用户数据")
    public String query(
            @ToolParam(name = "sql") String sql,
            UserContext ctx) {  // 自动注入，无需 @ToolParam
        return "用户 " + ctx.getUserId() + " 的数据";
    }

    // 工具挂起
    @Tool(name = "external_api", description = "调用外部 API")
    public ToolResultBlock callExternalApi(
            @ToolParam(name = "url") String url) {
        // 抛出异常，暂停执行
        throw new ToolSuspendException("等待外部 API 响应: " + url);
    }
}
