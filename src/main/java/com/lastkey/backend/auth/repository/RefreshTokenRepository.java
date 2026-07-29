package com.lastkey.backend.auth.repository;

import com.lastkey.backend.auth.entity.RefreshToken;
import com.lastkey.backend.user.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface RefreshTokenRepository extends JpaRepository<RefreshToken, UUID> {

    Optional<RefreshToken> findByToken(String token);

    Optional<RefreshToken> findByUser(User user);

    void deleteByUser(User user);

}