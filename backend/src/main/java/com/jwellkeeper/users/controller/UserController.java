package com.jwellkeeper.users.controller;

import com.jwellkeeper.common.api.ApiResponse;
import com.jwellkeeper.common.pagination.PageResponse;
import com.jwellkeeper.users.dto.CreateStaffRequest;
import com.jwellkeeper.users.dto.UpdateStaffRequest;
import com.jwellkeeper.users.dto.UserResponse;
import com.jwellkeeper.users.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/users/staff")
@PreAuthorize("hasRole('OWNER')")
public class UserController {

    private final UserService service;

    @PostMapping
    public ApiResponse<UserResponse> create(@Valid @RequestBody CreateStaffRequest request) {
        return ApiResponse.success("Staff user created", service.createStaff(request));
    }

    @GetMapping
    public ApiResponse<PageResponse<UserResponse>> list(Pageable pageable) {
        return ApiResponse.success("Staff users fetched", service.listStaff(pageable));
    }

    @PutMapping("/{id}")
    public ApiResponse<UserResponse> update(@PathVariable UUID id, @Valid @RequestBody UpdateStaffRequest request) {
        return ApiResponse.success("Staff user updated", service.updateStaff(id, request));
    }

    @DeleteMapping("/{id}")
    public ApiResponse<Void> deactivate(@PathVariable UUID id) {
        service.deactivateStaff(id);
        return ApiResponse.success("Staff user deactivated");
    }
}
