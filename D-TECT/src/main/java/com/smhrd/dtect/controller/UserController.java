package com.smhrd.dtect.controller;


import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PostMapping;

import com.smhrd.dtect.entity.Member;
import com.smhrd.dtect.service.UserService;

@Controller
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    @PostMapping("/signup")
    public String register(Member member) {
        userService.register(member);
        return "redirect:/login"; // 회원가입 후 로그인 페이지 이동
    }
    
}
