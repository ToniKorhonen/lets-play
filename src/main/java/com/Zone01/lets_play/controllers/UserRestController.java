package com.Zone01.lets_play.controllers;

import com.Zone01.lets_play.dto.UserDtos.*;
import com.Zone01.lets_play.service.UserService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PostAuthorize;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import jakarta.annotation.security.PermitAll;
import java.util.List;

@RestController
@RequestMapping("/api/users")
public class UserRestController {

    private final UserService service;

    @Autowired
    public UserRestController(UserService service) {
        this.service = service;
    }

    @PermitAll
    @PostMapping
    public ResponseEntity<UserResponse> create(@Valid @RequestBody CreateUserRequest req) {
        return ResponseEntity.status(201).body(service.create(req));
    }

    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping
    public List<UserResponse> list() {
        return service.list();
    }

    @PreAuthorize("hasRole('ADMIN')")
    @PostAuthorize("hasRole('ADMIN') or returnObject.email == authentication.name")
    @GetMapping("/{id}")
    public UserResponse get(@PathVariable String id) {
        return service.get(id);
    }

    @PreAuthorize("hasRole('ADMIN')")
    @PutMapping("/{id}")
    public UserResponse update(@PathVariable String id,
                               @Valid @RequestBody UpdateUserRequest req) {
        return service.update(id, req, true);
    }

    @PreAuthorize("hasRole('ADMIN') or @userService.isCurrentUser(#id, authentication)")
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable String id, Authentication auth) {
        service.delete(id);
        return ResponseEntity.noContent().build();
    }
}
