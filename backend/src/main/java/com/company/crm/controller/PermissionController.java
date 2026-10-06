package com.company.crm.controller;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.company.crm.service.PermissionService;
import com.company.crm.vo.permission.PermissionTreeVO;
import com.company.crm.vo.permission.PermissionListVO;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/permissions")
@RequiredArgsConstructor
public class PermissionController {
    
    private final PermissionService permissionService;

    @GetMapping
    @PreAuthorize("hasRole('SUPER_ADMIN') and hasAuthority('permission:list')")
    public ResponseEntity<List<PermissionTreeVO>> getPermissionTree(){
        return ResponseEntity.ok(permissionService.getPermissionTree());
    }

    @GetMapping("/list")
    @PreAuthorize("hasRole('SUPER_ADMIN') and hasAuthority('permission:list')")
    public ResponseEntity<List<PermissionListVO>> getPermissionList(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Long moduleId,
            @RequestParam(required = false) @Min(0) @Max(1) Integer status
    ) {
        return ResponseEntity.ok(permissionService.getPermissionList(keyword, moduleId, status));
    }
}
