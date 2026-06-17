package com.example.shrimpiot.repository;

import com.example.shrimpiot.model.Pond;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface PondRepository extends JpaRepository<Pond, Long> {
}
