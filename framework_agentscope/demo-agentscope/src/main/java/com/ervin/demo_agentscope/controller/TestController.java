package com.ervin.demo_agentscope.controller;

import com.ervin.demo_agentscope.agent.StructuredOutputAgent;
import com.ervin.demo_agentscope.model.ProductInfo;
import io.agentscope.core.message.Msg;
import io.agentscope.core.message.MsgRole;
import io.agentscope.core.message.TextBlock;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/test")
public class TestController {
    @Autowired
    StructuredOutputAgent agent;

    @GetMapping("/001")
    public void test001(){
        try {
            Msg userMsg = Msg.builder()
                    .role(MsgRole.USER)
                    .content(TextBlock.builder().text("Hello!").build())
                    .build();
            ProductInfo data = (ProductInfo) agent.go(userMsg, ProductInfo.class);

            // 业务验证
            if (data.price < 0) {
                throw new IllegalArgumentException("价格无效");
            }
        } catch (Exception e) {
            System.err.println("处理失败: " + e.getMessage());
        }
    }
}
