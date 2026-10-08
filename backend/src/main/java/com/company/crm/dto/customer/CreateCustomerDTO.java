package com.company.crm.dto.customer;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

/** 客户创建输入边界；归属、编号和审计字段只能由后端生成。 */
@Data
public class CreateCustomerDTO {

    @NotBlank(message = "客户名称不能为空")
    @Size(max = 200, message = "客户名称不能超过200个字符")
    private String customerName;

    @Pattern(regexp = "^(ENTERPRISE|INDIVIDUAL)?$", message = "客户类型不合法")
    private String customerType;

    @Pattern(regexp = "^(A|B|C|D)?$", message = "客户等级不合法")
    private String customerLevel;

    @Size(max = 64, message = "所属行业不能超过64个字符")
    private String industry;

    @Size(max = 64, message = "客户来源不能超过64个字符")
    private String source;

    @Size(max = 32, message = "联系电话不能超过32个字符")
    private String phone;

    @Email(message = "邮箱格式不正确")
    @Size(max = 128, message = "邮箱不能超过128个字符")
    private String email;

    @Size(max = 64, message = "省份不能超过64个字符")
    private String province;

    @Size(max = 64, message = "城市不能超过64个字符")
    private String city;

    @Size(max = 500, message = "详细地址不能超过500个字符")
    private String address;

    @Pattern(regexp = "^(POTENTIAL|ACTIVE|INACTIVE)?$", message = "客户状态不合法")
    private String status;

    @Size(max = 1000, message = "备注不能超过1000个字符")
    private String remark;
}
