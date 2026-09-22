package com.company.crm.controller;

import com.company.crm.dto.role.CreateRoleDTO;
import com.company.crm.dto.role.UpdateRolePermissionsDTO;
import com.company.crm.service.RoleService;
import com.company.crm.vo.role.RoleListVO;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.PutMapping;

import java.util.List;

/**
 * 角色管理接口入口。Controller 只负责 HTTP 协议转换，查询逻辑交由 Service 和 Mapper。
 */
@RestController
@RequestMapping("/roles")
@RequiredArgsConstructor
public class RoleController {

    private final RoleService roleService;

    @GetMapping
    @PreAuthorize("hasAuthority('role:list')")
    public ResponseEntity<List<RoleListVO>> listRoles() {
        return ResponseEntity.ok(roleService.listRoles());
    }

    @GetMapping("/{roleId}/permissions")
    @PreAuthorize("hasRole('SUPER_ADMIN') and hasAuthority('role:assign_permission')")
    public ResponseEntity<List<String>> getRolePermissionIds(@PathVariable Long roleId) {
        List<String> permissionIds = roleService.getRolePermissionIds(roleId).stream()
                .map(String::valueOf)
                .toList();
        return ResponseEntity.ok(permissionIds);
    }

    @PutMapping("/{roleId}/permissions")
    @PreAuthorize("hasRole('SUPER_ADMIN') and hasAuthority('role:assign_permission')")
    public ResponseEntity<Void> updateRolePermissions(
            @PathVariable Long roleId,
            @Valid @RequestBody UpdateRolePermissionsDTO dto
    ) {
        roleService.updateRolePermissions(roleId, dto.getPermissionIds());
        return ResponseEntity.noContent().build();
    }

    @PostMapping
    @PreAuthorize("hasRole('SUPER_ADMIN') and hasAuthority('role:create')")
    public ResponseEntity<Long> createRole(@Valid @RequestBody CreateRoleDTO dto) {
        return ResponseEntity.status(HttpStatus.CREATED).body(roleService.createRole(dto));
    }
}
