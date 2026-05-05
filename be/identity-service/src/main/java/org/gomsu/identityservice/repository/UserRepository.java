package org.gomsu.identityservice.repository;

import org.gomsu.identityservice.entity.RoleName;
import org.gomsu.identityservice.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User,Long> {
    boolean existsByUsername(String name);
    boolean existsByEmail(String email);
    boolean existsByPhone(String phone);
    Optional<User> findByUsername(String name);
    Optional<User> findByEmail(String email);
    Optional<User> findByPhone(String phone);

    @Query("SELECT u FROM User u LEFT JOIN u.roles r WHERE " +
            "(:role IS NULL OR r.roleName = :role) AND " +
            "(:keyword IS NULL OR :keyword = '' OR " +
            "CONCAT(u.id, '') LIKE CONCAT('%', :keyword, '%') OR " +
            "LOWER(u.username) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
            "u.phone LIKE CONCAT('%', :keyword, '%') OR " +
            "LOWER(u.email) LIKE LOWER(CONCAT('%', :keyword, '%')))")
    java.util.List<User> searchUsers(@Param("keyword") String keyword, @Param("role") RoleName role);
}
