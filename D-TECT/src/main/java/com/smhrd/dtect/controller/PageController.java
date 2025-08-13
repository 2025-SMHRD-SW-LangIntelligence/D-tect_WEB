package com.smhrd.dtect.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

import com.smhrd.dtect.entity.Member;

@Controller
public class PageController {
	
	Member member = new Member();
	
	// 로그인 페이지
	@GetMapping(value = "/LoginPage")
	public String login() {
		
		return "public/index";
		
	}
	
	// 사용자 메인 페이지
	@GetMapping(value = "/UserMainPage")
	public String user() {
		
		return "user/list";
		
	}
	
	// 전문가 메인 페이지
	@GetMapping(value = "ExpertMainPage")
	public String expert() {
		
		return "expert/list";
		
	}
	
	// 관리자 메인 페이지
	@GetMapping(value = "AdminMainPage")
	public String admin() {
		
		return "admin/list";
		
	}
	
	
	
}
