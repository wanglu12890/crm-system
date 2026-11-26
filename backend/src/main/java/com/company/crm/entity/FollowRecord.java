package com.company.crm.entity;
import lombok.Data;
import com.baomidou.mybatisplus.annotation.*;
@Data
@TableName("follow_record")
public class FollowRecord {
 @TableId(type = IdType.ASSIGN_ID) private Long id;
 private String targetType;
 private Long targetId;
 private Long contactId;
 private String followType;
 private String content;
 private java.time.LocalDateTime followAt;
 private java.time.LocalDateTime nextFollowAt;
 private Long ownerId;
 private Long createdBy;
 private Long updatedBy;
 private java.time.LocalDateTime createdAt;
 private java.time.LocalDateTime updatedAt;
 @TableLogic private Integer deleted;
 @Version private Integer version;
}
