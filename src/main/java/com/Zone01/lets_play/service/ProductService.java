package com.Zone01.lets_play.service;

import com.Zone01.lets_play.Mongo_repisitory.ProductRepository;
import com.Zone01.lets_play.Mongo_repisitory.UserRepository;
import com.Zone01.lets_play.dto.ProductDtos.*;
import com.Zone01.lets_play.exception.ProductNotFoundException;
import com.Zone01.lets_play.exception.AccessDeniedBusinessException;
import com.Zone01.lets_play.models.Product;
import com.Zone01.lets_play.models.User;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;

@Service
public class ProductService {

    private final ProductRepository repo;
    private final UserRepository userRepo;

    @Autowired
    public ProductService(ProductRepository repo, UserRepository userRepo) {
        this.repo = repo;
        this.userRepo = userRepo;
    }

    @Transactional
    public ProductResponse create(CreateProductRequest req, String ownerUserId) {
        Product p = new Product();
        p.setName(s(req.name()));
        p.setDescription(s(req.description()));
        p.setPrice(req.price());
        p.setUserId(ownerUserId);
        repo.save(p);
        return toResponse(p);
    }

    public List<ProductResponse> publicList() {
        return repo.findAll().stream().map(this::toResponse).toList();
    }

    public ProductResponse get(String id) {
        return toResponse(find(id));
    }

    @Transactional
    public ProductResponse update(String id, UpdateProductRequest req, String requesterUserId, boolean isAdmin) {
        Product p = find(id);
        checkOwnership(p, requesterUserId, isAdmin);
        p.setName(s(req.name()));
        p.setDescription(s(req.description()));
        p.setPrice(req.price());
        repo.save(p);
        return toResponse(p);
    }

    @Transactional
    public void delete(String id, String requesterUserId, boolean isAdmin) {
        Product p = find(id);
        checkOwnership(p, requesterUserId, isAdmin);
        repo.delete(p);
    }

    private Product find(String id) {
        return repo.findById(id).orElseThrow(() -> new ProductNotFoundException(id, "retrieval"));
    }

    private void checkOwnership(Product p, String requesterUserId, boolean isAdmin) {
        if (!isAdmin && (p.getUserId() == null || !p.getUserId().equals(requesterUserId))) {
            throw new AccessDeniedBusinessException("Not owner of product");
        }
    }

    private ProductResponse toResponse(Product p) {
        String ownerName = "Unknown";
        if (p.getUserId() != null) {
            User owner = userRepo.findByEmail(p.getUserId());
            if (owner != null && owner.getName() != null) {
                ownerName = owner.getName();
            }
        }
        return new ProductResponse(p.getId(), p.getName(), p.getDescription(), p.getPrice(), p.getUserId(), ownerName);
    }

    private String s(String v) {
        if (v == null) return null;
        return v.trim();
    }
}
