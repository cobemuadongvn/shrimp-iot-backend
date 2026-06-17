package com.example.shrimpiot.repository;

import com.example.shrimpiot.model.Pond;
import com.example.shrimpiot.model.UserAccount;
import com.example.shrimpiot.model.UserPondAccess;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UserPondAccessRepository extends JpaRepository<UserPondAccess, Long> {
    List<UserPondAccess> findByUser(UserAccount user);
    List<UserPondAccess> findByPond(Pond pond);
    boolean existsByUserAndPond(UserAccount user, Pond pond);
    Optional<UserPondAccess> findByUserAndPond(UserAccount user, Pond pond);
    void deleteByUserAndPond(UserAccount user, Pond pond);
}
