// src/main/java/com/smhrd/dtect/controller/UserRestController.java
package com.smhrd.dtect.controller;

import com.smhrd.dtect.SignupRequestDTO;
import com.smhrd.dtect.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.Map;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/members")
public class UserRestController {

    private final UserService userService;

    // 회원가입 (일반 + 전문가) : FormData(data: JSON, certificationFile: File)
    @PostMapping(value = "/signup", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<?> signup(
            @RequestPart("data") SignupRequestDTO req,
            @RequestPart(value = "certificationFile", required = false) MultipartFile file
    ) {
        try {
            userService.registerMember(req, file);
            return ResponseEntity.ok(Map.of("success", true));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "message", e.getMessage()
            ));
        }
    }

    @PostMapping("/check-username")
    public ResponseEntity<Map<String, Boolean>> checkUsername(@RequestParam String username) {
        boolean available = userService.isUsernameAvailable(username);
        return ResponseEntity.ok(Map.of("available", available));
    }

    @PostMapping("/send-code")
    public ResponseEntity<Map<String, Object>> sendCode(@RequestBody Map<String, String> request) {
        String email = request.get("email");
        userService.sendVerificationEmail(email);
        return ResponseEntity.ok(Map.of("success", true));
    }

    @PostMapping("/verify-code")
    public ResponseEntity<Map<String, Object>> verifyCode(@RequestBody Map<String, String> request) {
        String email = request.get("email");
        String code  = request.get("code");
        boolean ok = userService.verifyEmailCode(email, code);
        return ResponseEntity.ok(Map.of("success", ok));
    }
}
