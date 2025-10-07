package com.Zone01.lets_play.controllers;

import com.Zone01.lets_play.dto.UserDtos.*;
import com.Zone01.lets_play.service.UserService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/users")
public class UserRestController {

    private final UserService service;

    public UserRestController(UserService service) {
        this.service = service;
    }

    @PostMapping
    public ResponseEntity<UserResponse> create(@Valid @RequestBody CreateUserRequest req) {
        return ResponseEntity.status(201).body(service.create(req));
    }

    @GetMapping
    public List<UserResponse> list() {
        return service.list();
    }

    @GetMapping("/{id}")
    public UserResponse get(@PathVariable String id) {
        return service.get(id);
    }

    @PutMapping("/{id}")
    public UserResponse update(@PathVariable String id,
                               @Valid @RequestBody UpdateUserRequest req) {
        return service.update(id, req, true);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable String id) {
        service.delete(id);
        return ResponseEntity.noContent().build();
    }
}
