package com.company.crm.dto.ai;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record AiAnalysisRequest(
        @NotBlank(message = "分析问题不能为空")
        @Size(max = 1000, message = "分析问题长度不能超过1000个字符")
        String question
) {
    @Override
    public String toString() {
        return "AiAnalysisRequest[question=<redacted>]";
    }
}
