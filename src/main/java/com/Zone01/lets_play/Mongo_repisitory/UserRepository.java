package com.Zone01.lets_play.Mongo_repisitory;

import com.Zone01.lets_play.models.User;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface UserRepository extends MongoRepository<User, String> {
    User findByEmail(String email);
}
