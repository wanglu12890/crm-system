package com.company.crm.controller;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.company.crm.service.PermissionService;
import com.company.crm.vo.permission.PermissionTreeVO;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/permissions")
@RequiredArgsConstructor
public class PermissionController {
    
    private final PermissionService permissionService;

    @GetMapping
    @PreAuthorize("hasAuthority('permission:list')")
    public ResponseEntity<List<PermissionTreeVO>> getPermissionTree(){
        return ResponseEntity.ok(permissionService.getPermissionTree());
    }
}
