package com.smhrd.dtect.controller;

import java.sql.Timestamp;
import java.time.Instant;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

import com.smhrd.dtect.entity.MemRole;
import com.smhrd.dtect.entity.Member;
import com.smhrd.dtect.repository.UserRepository;

@Controller
public class UserController {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;
    
    @PostMapping("/register.do")
    public void register(@RequestParam Long memIdx, @RequestParam String username, @RequestParam String password, @RequestParam String name, @RequestParam String phone, @RequestParam String address, @RequestParam String email, @RequestParam String terms_agree, @RequestParam MemRole role, @RequestParam String office_name, @RequestParam Timestamp joined_at) {
    	
    	Member member = new Member();
    	role = MemRole.USER;
    	member.setUsername(username);
    	member.setPassword(password);
    	member.setName(name);
    	member.setPhone(phone);
    	member.setAddress(address);
    	member.setEmail(email);
    	member.setTerms_agree(terms_agree);	
    	member.setMem_role(role);
    	member.setOffice_name(null);				// 전문가 등록 페이지에서 추가 예정이므로 회원가입 시에는 null로 처리
    	member.setJoined_at(joined_at);
    	
    }
    
}
