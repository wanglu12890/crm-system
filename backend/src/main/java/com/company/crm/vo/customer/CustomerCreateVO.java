package com.company.crm.vo.customer;

/** 创建成功后只返回定位新客户所需的非敏感标识。 */
public record CustomerCreateVO(String id, String customerNo) {
}
