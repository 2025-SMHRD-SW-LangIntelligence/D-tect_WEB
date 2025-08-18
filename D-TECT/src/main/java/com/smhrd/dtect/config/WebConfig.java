package com.smhrd.dtect.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebConfig implements WebMvcConfigurer {
	
	//WebMvcConfigurer는 Spring MVC에서 웹 애플리케이션의 설정을 커스터마이징할 때 사용하는 인터페이스
	//Spring Boot의 기본 설정을 유지하면서 필요한 부분만 수정할 수 있습니다.
	
    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        registry.addResourceHandler("/uploads/**") 			// 사용자가 브라우저에서 요청하는 접근 경로
                .addResourceLocations("file:/home/git/uploads/");	// 실제로는 서버의 해당 경로에 있는 이미지를 찾아주세요
    }
}