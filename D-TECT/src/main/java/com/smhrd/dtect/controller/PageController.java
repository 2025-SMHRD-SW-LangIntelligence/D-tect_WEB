package com.smhrd.dtect.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class PageController {

    @GetMapping("/LoginPage")
    public String login() {
        return "public/index";  // 로그인 JSP
    }

    @GetMapping("/signup")
    public String signup() {
        return "public/signup"; // 회원가입 JSP
    }

    @GetMapping("/UserMainPage")
    public String user() {
        return "user/list";     // 사용자 메인
    }

    @GetMapping("/expert/list")
    public String expert() {
        return "expert/list";   // 전문가 메인
    }

    @GetMapping("/admin/list")
    public String admin() {
        return "admin/list";    // 관리자 메인
    }
}
