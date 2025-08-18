package com.smhrd.dtect.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;


@Configuration // 환경 설정
public class FileUploadConfig {
	
	// upload 될 경로 설정
	
	@Value("${file.upload-dir}")
	private String uploadDir;
	
	public String getUploadDir() {
		
		return uploadDir;
		
	}
	
}
