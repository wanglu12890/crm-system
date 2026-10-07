package com.company.crm.vo.customer;

import java.time.LocalDateTime;

/** Stable list projection; BIGINT identifiers are returned as strings. */
public record CustomerListVO(
        String id,
        String customerNo,
        String customerName,
        String customerType,
        String customerLevel,
        String industry,
        String status,
        String ownerId,
        String ownerName,
        LocalDateTime updatedAt
) {
}
