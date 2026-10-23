package com.company.crm.controller;

import com.company.crm.dto.ai.CustomerRiskRequest;
import com.company.crm.security.SecurityUser;
import com.company.crm.service.CustomerRiskAnalysisService;
import com.company.crm.vo.ai.RiskAnalysisVO;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/customer-risks/analyses")
@RequiredArgsConstructor
public class AiCustomerRiskController {
    private final CustomerRiskAnalysisService analysis;

    @PostMapping
    @PreAuthorize("hasAuthority('ai:analysis') and hasAuthority('customer:list') and hasAuthority('follow:list')")
    public RiskAnalysisVO analyze(@RequestBody CustomerRiskRequest request, @AuthenticationPrincipal SecurityUser user) {
        return analysis.analyze(user == null ? null : user.getUserId(), request);
    }
}
