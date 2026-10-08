package com.company.crm.utils;

import com.baomidou.mybatisplus.core.toolkit.IdWorker;
import org.springframework.stereotype.Component;

/** 使用线程安全的雪花 ID 同时生成客户主键和全局唯一客户编号。 */
@Component
public class CustomerNumberGenerator {

    private static final String PREFIX = "KH";

    public Long nextId() {
        return IdWorker.getId();
    }

    public String customerNo(Long customerId) {
        return PREFIX + customerId;
    }
}
