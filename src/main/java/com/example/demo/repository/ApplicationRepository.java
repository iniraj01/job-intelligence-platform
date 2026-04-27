package com.example.demo.repository;

import com.example.demo.model.Application;
import org.springframework.data.mongodb.repository.MongoRepository;
import java.util.List;

public interface ApplicationRepository extends MongoRepository<Application, String> {
    List<Application> findByUserIdOrderByAppliedAtDesc(String userId);
}
