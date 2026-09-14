package com.company.crm.dto.role;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.List;

@Data
public class UpdateRolePermissionsDTO {

    @NotNull(message = "权限ID列表不能为空")
    @Valid
    private List<@NotNull(message = "权限ID不能为空") Long> permissionIds;
}
