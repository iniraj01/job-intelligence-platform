package com.example.demo.repository;

import com.example.demo.model.OtpData;
import org.springframework.data.mongodb.repository.MongoRepository;
import java.util.Optional;

public interface OtpDataRepository extends MongoRepository<OtpData, String> {
    Optional<OtpData> findByEmail(String email);

    void deleteByEmail(String email);
}
