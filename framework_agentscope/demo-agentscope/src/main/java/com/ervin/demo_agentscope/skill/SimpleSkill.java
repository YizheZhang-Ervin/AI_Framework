package com.ervin.demo_agentscope.skill;

import io.agentscope.core.skill.AgentSkill;
import io.agentscope.core.skill.util.SkillUtil;

import java.util.Map;

public class SimpleSkill {

    // 用builder创建
    public static AgentSkill dataAnalysis1() {
        AgentSkill skill = AgentSkill.builder()
                .name("data_analysis")
                .description("Use when analyzing data...")
                .putMetadata("homepage", "https://example.com/docs")
                .skillContent("# Data Analysis\n...")
                .addResource("references/formulas.md", "# 常用公式\n...")
                .source("custom")
                .build();
        return skill;
    }

    // 用markdown创建
    public static AgentSkill dataAnalysis2() {
        String skillMd = """
                ---
                name: data_analysis
                description: Use this skill when analyzing data, calculating statistics, or generating reports
                ---
                # 技能名称
                Content...
                """;

        Map<String, String> resources = Map.of(
                "references/formulas.md", "# 常用公式\n...",
                "examples/sample.csv", "name,value\nA,100\nB,200"
        );

        AgentSkill skill = SkillUtil.createFrom(skillMd, resources);
        return skill;
    }

    // 直接构建
    public static AgentSkill dataAnalysis3() {
        Map<String, String> resources = Map.of(
                "references/formulas.md", "# 常用公式\n...",
                "examples/sample.csv", "name,value\nA,100\nB,200"
        );
        AgentSkill skill = new AgentSkill(
                "data_analysis",                    // name
                "Use when analyzing data...",       // description
                "# Data Analysis\n...",             // skillContent
                resources                            // resources (可为 null)
        );
        return skill;
    }
}
