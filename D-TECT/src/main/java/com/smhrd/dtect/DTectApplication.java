package com.smhrd.dtect;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;
import org.springframework.scheduling.annotation.EnableScheduling;

@EnableScheduling
@SpringBootApplication
@ConfigurationPropertiesScan(basePackages = "com.smhrd.dtect.config") // ✅ app.model / app.pdf 바인딩 활성화
public class DTectApplication {

	public static void main(String[] args) {
		SpringApplication.run(DTectApplication.class, args);
	}

}
