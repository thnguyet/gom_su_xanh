package org.gomsu.identityservice.repository;

import org.gomsu.identityservice.entity.Otp;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface OtpRepository extends JpaRepository<Otp, Long> {
    Optional<Otp> findTopByEmailOrderByExpiryTimeDesc(String email);
    void deleteByEmail(String email);
}
