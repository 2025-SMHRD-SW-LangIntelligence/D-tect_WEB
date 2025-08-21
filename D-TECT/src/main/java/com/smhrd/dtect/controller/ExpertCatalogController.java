package com.smhrd.dtect.controller;

import com.smhrd.dtect.dto.LawyerCardDto;
import com.smhrd.dtect.service.ExpertCatalogService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/experts")
public class ExpertCatalogController {

    private final ExpertCatalogService expertCatalogService;

    @GetMapping
    public List<LawyerCardDto> list(
            @RequestParam(required = false) String q,
            @RequestParam(required = false) String skill,
            @RequestParam(defaultValue = "rec") String sort
    ) {
        return expertCatalogService.list(q, skill, sort);
    }
}
