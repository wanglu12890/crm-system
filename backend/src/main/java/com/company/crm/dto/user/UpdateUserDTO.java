package com.company.crm.dto.user;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.List;

@Data
public class UpdateUserDTO {

    @NotBlank(message = "真实姓名不能为空")
    @Size(max = 64, message = "真实姓名不能超过64个字符")
    private String realName;

    @Pattern(regexp = "^$|^1\\d{10}$", message = "手机号格式不正确")
    private String phone;

    @NotNull(message = "用户状态不能为空")
    @Min(value = 0, message = "用户状态只能为0或1")
    @Max(value = 1, message = "用户状态只能为0或1")
    private Integer status;

    @NotEmpty(message = "请至少选择一个角色")
    private List<@NotNull(message = "角色ID不能为空") Long> roleIds;
}
