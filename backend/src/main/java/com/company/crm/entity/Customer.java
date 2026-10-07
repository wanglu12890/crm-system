package com.company.crm.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.annotation.Version;
import lombok.Data;

import java.time.LocalDateTime;

/** Customer persistence model. API responses use dedicated VO types. */
@Data
@TableName("customer")
public class Customer {

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;
    private String customerNo;
    private String customerName;
    private String customerType;
    private String customerLevel;
    private String industry;
    private String source;
    private String phone;
    private String email;
    private String province;
    private String city;
    private String address;
    private Long ownerId;
    private String status;
    private String remark;
    private Long createdBy;
    private Long updatedBy;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    @TableLogic
    private Integer deleted;

    @Version
    private Integer version;
}
