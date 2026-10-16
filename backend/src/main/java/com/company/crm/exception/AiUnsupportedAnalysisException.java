package com.company.crm.exception;

public class AiUnsupportedAnalysisException extends RuntimeException {
    public AiUnsupportedAnalysisException(String message) {
        super(message);
    }

    public AiUnsupportedAnalysisException() {
        super("目前仅支持当前有效授权范围内的客户总量、客户等级分布，时间为全部时间、本月、上月或近30天；指定查询范围须与实际范围一致，暂不支持主动缩小到本人、指定人员或部门");
    }
}
