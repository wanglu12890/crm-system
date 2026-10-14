package com.company.crm.controller;

import com.company.crm.dto.ai.AiAnalysisRequest;
import com.company.crm.security.SecurityUser;
import com.company.crm.service.AiAnalysisService;
import com.company.crm.vo.AiAnalysisVO;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/ai/analysis")
@RequiredArgsConstructor
public class AiAnalysisController {
    private final AiAnalysisService aiAnalysisService;

    @PostMapping
    @PreAuthorize("hasAuthority('ai:analysis') and hasAuthority('customer:list')")
    public ResponseEntity<AiAnalysisVO> analyze(
            @Valid @RequestBody AiAnalysisRequest request,
            @AuthenticationPrincipal SecurityUser securityUser
    ) {
        Long authenticatedUserId = securityUser == null ? null : securityUser.getUserId();
        return ResponseEntity.ok(aiAnalysisService.analyze(authenticatedUserId, request.question()));
    }
}
