package com.company.crm.entity;
import lombok.Data;
import com.baomidou.mybatisplus.annotation.*;
@Data
@TableName("contact")
public class Contact {
 @TableId(type = IdType.ASSIGN_ID) private Long id;
 private Long customerId;
 private String contactName;
 private Integer gender;
 private String position;
 private String mobile;
 private String telephone;
 private String email;
 private Integer isPrimary;
 private String decisionRole;
 private java.time.LocalDate birthday;
 private String remark;
 private Long createdBy;
 private Long updatedBy;
 private java.time.LocalDateTime createdAt;
 private java.time.LocalDateTime updatedAt;
 @TableLogic private Integer deleted;
 @Version private Integer version;
}
