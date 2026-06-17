package com.example.shrimpiot.repository;

import com.example.shrimpiot.model.RoleName;
import com.example.shrimpiot.model.UserAccount;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface UserAccountRepository extends JpaRepository<UserAccount, Long> {

    Optional<UserAccount> findByUsername(String username);

    Optional<UserAccount> findByEmail(String email);

    boolean existsByUsername(String username);

    boolean existsByEmail(String email);

    long countByRoleAndActiveTrue(RoleName role);

    List<UserAccount> findByRoleAndActiveTrue(RoleName role);
}

