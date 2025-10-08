package com.Zone01.lets_play.service;

import com.Zone01.lets_play.Mongo_repisitory.UserRepository;
import com.Zone01.lets_play.Mongo_repisitory.ProductRepository;
import com.Zone01.lets_play.dto.UserDtos.*;
import com.Zone01.lets_play.exception.*;
import com.Zone01.lets_play.models.User;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;

@Service
public class UserService {

    private final UserRepository repo;
    private final ProductRepository productRepo;
    private final PasswordEncoder encoder;

    @Autowired
    public UserService(UserRepository repo, ProductRepository productRepo, PasswordEncoder encoder) {

        this.repo = repo;
        this.productRepo = productRepo;
        this.encoder = encoder;
    }

    @Transactional
    public UserResponse create(CreateUserRequest req) {
        if (repo.findByEmail(req.email().toLowerCase()) != null) {
            throw new DuplicateResourceException("Email already in use");
        }
        if (req.role() != null && !req.role().isBlank() && !req.role().equalsIgnoreCase("USER") && !req.role().equalsIgnoreCase("ADMIN")) {
            throw new IllegalArgumentException("Role must be USER or ADMIN");
        }

        User u = new User();
        u.setName(sanitize(req.name()));
        u.setEmail(req.email());
        u.setPassword(encoder.encode(req.password()));
        u.setRole((req.role() == null || req.role().isBlank()) ? "USER" : req.role().toUpperCase());
        repo.save(u);
        return toResponse(u);
    }

    public List<UserResponse> list() {
        return repo.findAll().stream().map(this::toResponse).toList();
    }

    public UserResponse get(String id) {
        return toResponse(find(id));
    }

    @Transactional
    public UserResponse update(String id, UpdateUserRequest req, boolean allowRoleChange) {
        User u = find(id);
        u.setName(sanitize(req.name()));

        if (allowRoleChange && req.role() != null && !req.role().isBlank()) {
            u.setRole(req.role().toUpperCase());
        }
        repo.save(u);
        return toResponse(u);
    }

    @Transactional
    public UserResponse update(String id, UpdateProfileRequest req) {
        User u = find(id);
        if (req.name() != null && !req.name().isBlank()) {
            u.setName(sanitize(req.name()));
        }
        if (req.password() != null && !req.password().isBlank()) {
            u.setPassword(encoder.encode(req.password()));
        }
        repo.save(u);
        return toResponse(u);
    }

    @Transactional
    public void delete(String id) {
        User u = find(id);
        // DELETE CASCADE - supprimer tous les produits de l'utilisateur
        productRepo.findByUserId(u.getEmail()).forEach(productRepo::delete);
        // Puis supprimer l'utilisateur
        repo.delete(u);
    }

    public boolean isCurrentUser(String userId, org.springframework.security.core.Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()) {
            return false;
        }
        User user = find(userId);
        String currentUserEmail = authentication.getName();
        return user.getEmail().equals(currentUserEmail);
    }

    private User find(String id) {
        return repo.findById(id).orElseThrow(() -> new UserNotFoundException(id, "retrieval"));
    }

    private UserResponse toResponse(User u) {
        return new UserResponse(u.getId(), u.getName(), u.getEmail(), u.getRole());
    }

    private String sanitize(String v) {
        if (v == null) return null;
        if (v.contains("$") || v.contains("{") || v.contains("}")) {
            throw new IllegalArgumentException("Illegal characters");
        }
        return v.trim();
    }
}
