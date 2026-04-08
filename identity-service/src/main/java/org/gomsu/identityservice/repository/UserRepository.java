package org.gomsu.identityservice.repository;

import org.gomsu.identityservice.entity.RoleName;
import org.gomsu.identityservice.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User,Long> {
    boolean existsByUsername(String name);
    boolean existsByEmail(String email);
    boolean existsByPhone(String phone);
    Optional<User> findByUsername(String name);
    Optional<User> findByEmail(String email);
}
