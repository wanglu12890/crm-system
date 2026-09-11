package com.company.crm.dto.role;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class CreateRoleDTO {

    @NotBlank(message = "角色名称不能为空")
    @Size(max = 64, message = "角色名称不能超过64个字符")
    private String roleName;

    @NotBlank(message = "角色编码不能为空")
    @Size(max = 64, message = "角色编码不能超过64个字符")
    @Pattern(regexp = "^[A-Za-z][A-Za-z0-9_]*$", message = "角色编码只能包含字母、数字和下划线，且必须以字母开头")
    private String roleCode;

    @NotNull(message = "角色状态不能为空")
    @Min(value = 0, message = "角色状态只能为0或1")
    @Max(value = 1, message = "角色状态只能为0或1")
    private Integer status;

    @Size(max = 500, message = "角色备注不能超过500个字符")
    private String remark;
}
