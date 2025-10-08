package com.Zone01.lets_play.controllers;

import com.Zone01.lets_play.service.UserService;
import com.Zone01.lets_play.dto.UserDtos.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/web/admin")
public class AdminWebController {

    private final UserService userService;

    @Autowired
    public AdminWebController(UserService userService) {
        this.userService = userService;
    }

    @GetMapping("/users")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<UserResponse>> getAllUsers() {
        return ResponseEntity.ok(userService.list());
    }

    @DeleteMapping("/users/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> deleteUser(@PathVariable String id) {
        userService.delete(id);
        return ResponseEntity.ok(Map.of("message", "User deleted successfully"));
    }
}

