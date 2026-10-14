package com.company.crm.exception;

public class AiUnsupportedAnalysisException extends RuntimeException {
    public AiUnsupportedAnalysisException() {
        super("目前仅支持当前用户权限范围内的客户总量、客户等级分布，时间范围为全部时间、本月、上月或近30天；请明确统计需求");
    }
}
