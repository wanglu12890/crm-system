package com.company.crm.controller;

import com.company.crm.dto.user.CreateUserDTO;
import com.company.crm.dto.user.ResetUserPasswordDTO;
import com.company.crm.dto.user.UpdateUserDTO;
import com.company.crm.service.UserService;
import com.company.crm.vo.user.UserListVO;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import java.util.List;

@RestController
@RequestMapping("/users")
@RequiredArgsConstructor
public class UserController {
    private final UserService userService;

    @GetMapping
    @PreAuthorize("hasAuthority('user:list')")
    public ResponseEntity<List<UserListVO>> listUsers() {
        return ResponseEntity.ok(userService.listUsers());
    }

    @PostMapping
    @PreAuthorize("hasAuthority('user:create') and hasAuthority('user:assign_role')")
    public ResponseEntity<String> createUser(@Valid @RequestBody CreateUserDTO dto) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(String.valueOf(userService.createUser(dto)));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('user:update') and hasAuthority('user:assign_role')")
    public ResponseEntity<Void> updateUser(
            @PathVariable Long id,
            @Valid @RequestBody UpdateUserDTO dto
    ) {
        userService.updateUser(id, dto);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{id}/reset-password")
    @PreAuthorize("hasAuthority('user:reset_password')")
    public ResponseEntity<Void> resetPassword(
            @PathVariable Long id,
            @Valid @RequestBody ResetUserPasswordDTO dto
    ) {
        userService.resetPassword(id, dto);
        return ResponseEntity.ok().build();
    }
}
