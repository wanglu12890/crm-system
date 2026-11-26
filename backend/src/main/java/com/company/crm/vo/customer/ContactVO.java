package com.company.crm.vo.customer;
public record ContactVO(String id, String customerId, String contactName, Integer gender, String position, String mobile, String telephone, String email, Integer isPrimary, String decisionRole, java.time.LocalDate birthday, String remark) {}
