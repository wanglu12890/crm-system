package com.company.crm.vo.customer;



import java.time.LocalDateTime;public record FollowRecordVO(
        String id,
        String contactId,
        String followType,
        String content,
        LocalDateTime followAt,
        LocalDateTime nextFollowAt,
        String ownerId,
        String ownerName
) {
}
