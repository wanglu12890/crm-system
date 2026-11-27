package com.company.crm.vo.customer;



import java.time.LocalDateTime;public record CustomerDetailVO(
        String id,
        String customerNo,
        String customerName,
        String customerType,
        String customerLevel,
        String industry,
        String source,
        String phone,
        String email,
        String province,
        String city,
        String address,
        String ownerId,
        String ownerName,
        String status,
        String remark,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
}
