package com.company.crm.dto.user;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

/** 管理员重置其他用户密码时的请求参数。 */
@Data
public class ResetUserPasswordDTO {

    @NotBlank(message = "新密码不能为空")
    @Size(min = 6, max = 64, message = "密码长度应为6到64个字符")
    private String newPassword;
}
