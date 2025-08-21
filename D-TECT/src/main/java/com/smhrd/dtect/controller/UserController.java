package com.smhrd.dtect.controller;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

import com.smhrd.dtect.entity.MemRole;
import com.smhrd.dtect.entity.Member;
import com.smhrd.dtect.service.UserDetailsServiceImpl;
import com.smhrd.dtect.service.UserService;

import jakarta.servlet.http.HttpSession;

@Controller
public class UserController {
	
	private final UserService userService;
	
	public UserController(UserService userService) {
		this.userService = userService;
	}
	
	@PostMapping(value = "/login.do")
	public ResponseEntity<?> login(@RequestParam String username, @RequestParam String rawPassword, HttpSession session) {
		
		Optional<Member> memOpt = userService.findByUsername(username);
		if (!memOpt.isPresent()) {
			return errorResponse("username", "존재하지 않는 아이디입니다");
		}
		
		Member member = memOpt.get();
		
		if (!userService.checkPassword(member, rawPassword)) {
			return errorResponse("password", "비밀번호가 일치하지 않습니다");
		}
		
		session.setAttribute("member", member);
		
		String redirectUrl = determineRedirectUrl(member.getMemRole());
		return ResponseEntity.ok(redirectUrl);
				
	}

	private String determineRedirectUrl(MemRole role) {
		if (MemRole.EXPERT.equals(role)) {
			System.out.println("전문가");
			return "/farmerMainPage";
		} else if (MemRole.USER.equals(role)) {
			System.out.println("사용자");
			return "/consumerMainPage";
		} else if (MemRole.ADMIN.equals(role)) {
			System.out.println("관리자");
			return "/adminMainPage";
		}
		return "/";
	}
	

	private ResponseEntity<Map<String, String>> errorResponse(String field, String message) {
		Map<String, String> response = new HashMap<>();
		response.put("field", field);
		response.put("message", message);
		return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
	}
	
}
