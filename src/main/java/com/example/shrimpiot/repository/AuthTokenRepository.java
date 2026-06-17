package com.example.shrimpiot.repository;

import com.example.shrimpiot.model.AuthToken;
import com.example.shrimpiot.model.UserAccount;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.EntityGraph;

import java.util.List;
import java.util.Optional;

public interface AuthTokenRepository extends JpaRepository<AuthToken, Long> {

    @EntityGraph(attributePaths = "user")
    Optional<AuthToken> findByToken(String token);

    List<AuthToken> findByUserAndRevokedFalse(UserAccount user);
}
