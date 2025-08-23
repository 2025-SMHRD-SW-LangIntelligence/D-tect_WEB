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
	
    /* 공용 페이지 */
    
    @GetMapping(value = "/")
    public String start() {
    	return "public/start";					// 시작 페이지
    }
    
    @GetMapping(value = "/loginPage")
    public String login() {
        return "public/index";  				// 로그인 페이지
    }
    
    @GetMapping(value = "/chooseRolePage")
    public String chooseRole() {
    	return "public/signup";					// 회원 유형 선택 페이지
    }
    
    @GetMapping(value = "/findIdPage")
    public String findId() {
    	return "public/find_id";				// 아이디 찾기 페이지
    }
    
    @GetMapping(value = "/changePasswordPage")
    public String changePassword() {
    	return "public/change_password";		// 비밀번호 초기화 페이지
    }
    
    /* 일반 회원 페이지 */

    @GetMapping(value = "/joinUserPage")
    public String userSignup() {
        return "user/user_signup"; 				// 기본 회원 회원가입 페이지

    }

    @GetMapping(value = "/userTermPage")
    public String userTerm() {
        return "user/user_terms";				// 기본 회원 약관 페이지

    }
    
    @GetMapping(value = "/userMainPage")
    public String user() {
    	return "user/analysis";     			// 기본 회원 메인 페이지 
    }
    
    @GetMapping(value = "/capturePage")
    public String capture() {
    	return "capture/capture";				// 데이터 수집(캡쳐) 페이지
    }
    
    @GetMapping(value = "/userSchedulePage")
    public String userSchedule() {
    	return "user/user_reservation";			// 기본 회원 읿정 관리 페이지
    }
    
    @GetMapping(value = "/userMatchingPage")
    public String userMatching() {
    	return "user/lawyers_select";			// 기본 회원 전문가 매칭 페이지
    }
    
    @GetMapping(value = "/userMyinfoPage")
    public String userInfo() {
    	return "user/user_mypage";				// 일반 회원 내 정보 페이지
    }
    
    /* 전문가 회원 페이지 */
    
    @GetMapping(value = "/joinExpertPage")
    public String expertSignup() {
        return "expert/expert_signup"; 			// 전문가 회원 회원가입 페이지
    }
        
    @GetMapping(value = "/expertTermPage")	
    public String expertTerm() {	
    	return "expert/expert_terms";			// 전문가 회원 약관 페이지
    }

//    @GetMapping(value = "/expertMainPage")
//    public String expert() {
//        return "expert/expert_mypage";   		// 전문가 회원 메인 페이지
//    }
    
    @GetMapping(value = "/expertSchedulePage")
    public String expertSchedule() {
    	return "expert/expert_schedule";		// 전문가 회원 일정확인 페이지
    }
    
//    @GetMapping(value = "/expertMyinfoPage")
//    public String expertInfo() {
//    	return "expert/expert_mainpage";			// 전문가 내 정보 페이지
//    }
    
    /* 관리자 페이지 */

    @GetMapping(value = "/adminMainPage")
    public String admin() {
        return "admin/admin_page";    				// 관리자 메인 페이지
    }
    
    @GetMapping(value = "/adminUserBlockPage")
    public String userList(Model model) {
    	ArrayList<Member> userList = adminService.memberListView(model);
    	model.addAttribute("userlist", userList);
    	return "admin/userBlock";				// 관리자 회원 관리 페이지
    }
    
    @GetMapping(value = "/adminInfoUpdatePage")
    public String infoUpdate() {
    	return "admin/infoUpdate";				// 관리자 도움주는 정보 관리 페이지
    }
    
    /* 회원 전용 페이지(공용) */
    
    @GetMapping(value = "/chatBoardPage")
    public String chatBoard() {
    	return "chatboard/chat_board";
    }
}
