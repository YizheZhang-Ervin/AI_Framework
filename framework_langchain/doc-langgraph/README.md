# LangGraph

## 目录结构
- langgraph
    - agent-tool
        - 自定义工具函数/添加多个工具函数 + 智能体
        - 工具并联调用/工具串联调用
        - 创建带搜索功能的Agent
    - ecology
        - 工具/启动/配置/命令行
    - graph
        - 创建/运行/pydantic/pydantic用于stateGraph
        - 条件判断/循环语句/判断循环复合/子图
    - HIL
        - 人工干预/提示词+工具
        - 聊天机器人(基本/HITL/时间回溯/提示词+工具)
    - memory
        - 预构建 Agent 实现记忆存储
        - 聊天机器人
        - 长期记忆+跨线程召回
        - 消息裁剪
        - 消息总结
    - mcp
        - 服务端/配置/客户端
        - 天气助手MCP工具
    - multiagent
        - 员工外卖餐补助手