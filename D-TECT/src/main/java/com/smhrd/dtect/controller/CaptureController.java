package com.smhrd.dtect.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class CaptureController {

	// 캡처
    @GetMapping("/capture")
    public String capturePage() {
        return "capture/capture";
    } 
}
