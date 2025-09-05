package com.smhrd.dtect.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;

@Configuration
@EnableAsync
public class AsyncConfig {
    // PDF 생성을 비동기로 처리하기 위함
}
