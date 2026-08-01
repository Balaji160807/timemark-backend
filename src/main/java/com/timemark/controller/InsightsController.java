package com.timemark.controller;

import com.timemark.service.AiInsightsService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/insights")
@RequiredArgsConstructor
public class InsightsController {

    private final AiInsightsService aiInsightsService;

    /** HR / Manager / Admin only - enforced in SecurityConfig */
    @GetMapping("/team")
    public Map<String, String> teamInsight() {
        return Map.of("insight", aiInsightsService.teamInsight());
    }
}
