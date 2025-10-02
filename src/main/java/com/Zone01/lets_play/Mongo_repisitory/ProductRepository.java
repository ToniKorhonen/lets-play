package com.Zone01.lets_play.Mongo_repisitory;

import com.Zone01.lets_play.models.Product;
import org.springframework.data.mongodb.repository.MongoRepository;
import java.util.List;

public interface ProductRepository extends MongoRepository<Product, String> {
    List<Product> findByUserId(String userId);
}
