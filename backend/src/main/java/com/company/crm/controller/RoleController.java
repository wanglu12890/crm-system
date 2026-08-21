package com.company.crm.controller;

import com.company.crm.service.RoleService;
import com.company.crm.vo.role.RoleListVO;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

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
    public ResponseEntity<List<RoleListVO>> listRoles() {
        return ResponseEntity.ok(roleService.listRoles());
    }
}
