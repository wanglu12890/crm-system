package com.company.crm.controller;

import com.company.crm.service.UserService;
import com.company.crm.vo.user.UserListVO;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import java.util.List;

@RestController
@RequestMapping("/users")
@RequiredArgsConstructor
public class UserController {
    private final UserService userService;

    @GetMapping
    public ResponseEntity<List<UserListVO>> listUsers() {
        return ResponseEntity.ok(userService.listUsers());
    }
}