package com.ervin.demo_agentscope;

import io.agentscope.core.studio.StudioManager;
import jakarta.annotation.PreDestroy;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class DemoAgentscopeApplication {

	public static void main(String[] args) {
		// 初始化 Studio 连接
		StudioManager.init()
				.studioUrl("http://localhost:3000")
				.project("MyProject")
				.runName("demo_" + System.currentTimeMillis())
				.initialize()
				.block();
		SpringApplication.run(DemoAgentscopeApplication.class, args);
	}

	@PreDestroy
	public void destroy(){
		// 清理资源
		StudioManager.shutdown();
	}

}
