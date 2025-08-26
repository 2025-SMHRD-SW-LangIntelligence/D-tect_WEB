package com.smhrd.dtect.dto;

public record ModelResultDto (
		String user, 
		String text, 
		String score, 
		ModelClassificationDto classification) {}
