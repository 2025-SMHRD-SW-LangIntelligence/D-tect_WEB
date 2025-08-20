package com.smhrd.dtect.controller;

import java.util.ArrayList;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import com.smhrd.dtect.entity.Member;
import com.smhrd.dtect.service.AdminService;

@Controller
public class PageController {
	
    private final AdminService adminService;

    public PageController(AdminService adminService) {
        this.adminService = adminService;
    }
	
    @GetMapping(value = "/")
    public String start() {
    	return "public/start";						// 시작 페이지
    }
    
    @GetMapping(value = "/loginPage")
    public String login() {
        return "public/index";  			// 로그인 페이지
    }
    
    @GetMapping(value = "/chooseRolePage")
    public String chooseRole() {
    	return "public/chooseRole";			// 회원 유형 선택 페이지
    }
    

    @GetMapping(value = "/joinUserPage")
    public String userSignup() {
        return "user/signup"; 				// 기본 회원 회원가입 페이지
    }
    
    @GetMapping(value = "/joinExpertPage")
    public String expertSignup() {
        return "expert/signup"; 			// 전문가 회원 회원가입 페이지
    }

    @GetMapping(value = "/userMainPage")
    public String user() {
        return "user/list";     			// 기본 회원 메인 페이지 
    }

    @GetMapping(value = "/expertMainPage")
    public String expert() {
        return "expert/list";   			// 전문가 회원 메인 페이지
    }

    @GetMapping(value = "/adminMainPage")
    public String admin() {
        return "admin/list";    			// 관리자 메인 페이지
    }
    
    @GetMapping(value = "/adminUserBlockPage")
    public String userList(Model model) {
    	ArrayList<Member> userList = adminService.memberListView(model);
    	model.addAttribute("userlist", userList);
    	return "admin/userBlock";			// 관리자 회원 관리 페이지
    }
    
    @GetMapping(value = "/adminInfoUpdatePage")
    public String infoUpdate() {
    	return "admin/infoUpdate";
    }
}
