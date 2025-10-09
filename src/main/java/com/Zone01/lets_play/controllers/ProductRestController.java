package com.Zone01.lets_play.controllers;

import com.Zone01.lets_play.dto.ProductDtos.*;
import com.Zone01.lets_play.service.ProductService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import jakarta.annotation.security.PermitAll;
import java.util.List;

@RestController
@RequestMapping("/api/products")
public class ProductRestController {
    private final ProductService service;

    @Autowired
    public ProductRestController(ProductService service) {
        this.service = service;
    }

    // Public - accessible without authentication
    @PermitAll
    @GetMapping
    public List<ProductResponse> list() {
        return service.publicList();
    }

    // Public - accessible without authentication
    @PermitAll
    @GetMapping("/{id}")
    public ProductResponse get(@PathVariable String id) {
        return service.get(id);
    }

    @PreAuthorize("isAuthenticated()")
    @PostMapping
    public ResponseEntity<ProductResponse> create(@Valid @RequestBody CreateProductRequest req,
                                                  Authentication auth) {
        String userId = auth.getName(); // email comme identifiant logique
        return ResponseEntity.status(201).body(service.create(req, userId));
    }

    @PreAuthorize("isAuthenticated()")
    @PutMapping("/{id}")
    public ProductResponse update(@PathVariable String id,
                                  @Valid @RequestBody UpdateProductRequest req,
                                  Authentication auth) {
        boolean isAdmin = auth.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));
        return service.update(id, req, auth.getName(), isAdmin);
    }

    @PreAuthorize("isAuthenticated()")
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable String id, Authentication auth) {
        boolean isAdmin = auth.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));
        service.delete(id, auth.getName(), isAdmin);
        return ResponseEntity.noContent().build();
    }
}
