package com.company.crm.vo.customer;



import java.time.LocalDate;public record ContactVO(
        String id,
        String customerId,
        String contactName,
        Integer gender,
        String position,
        String mobile,
        String telephone,
        String email,
        Integer isPrimary,
        String decisionRole,
        LocalDate birthday,
        String remark
) {
}
