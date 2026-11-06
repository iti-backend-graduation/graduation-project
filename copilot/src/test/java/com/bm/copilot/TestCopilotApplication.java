package com.bm.copilot;

import org.springframework.boot.SpringApplication;

public class TestCopilotApplication {

	public static void main(String[] args) {
		SpringApplication.from(CopilotApplication::main).with(TestcontainersConfiguration.class).run(args);
	}

}
