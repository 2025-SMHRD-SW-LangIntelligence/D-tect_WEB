package com.smhrd.dtect.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PostMapping;

import com.smhrd.dtect.service.UserDetailsServiceImpl;

@Controller
public class UserController {
	
	private final UserDetailsServiceImpl userDetailsServiceImpl;
	
	public UserController(UserDetailsServiceImpl userDetailsServiceImpl) {
		this.userDetailsServiceImpl = userDetailsServiceImpl;
	}
	
	
}
