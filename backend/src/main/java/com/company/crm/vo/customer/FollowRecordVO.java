package com.company.crm.vo.customer;
public record FollowRecordVO(String id, String contactId, String followType, String content, java.time.LocalDateTime followAt, java.time.LocalDateTime nextFollowAt, String ownerId, String ownerName) {}
