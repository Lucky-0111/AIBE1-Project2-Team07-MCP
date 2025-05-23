package org.lucky0111.pettalkmcpserver.repository.auth;

import org.lucky0111.pettalkmcpserver.domain.entity.auth.RefreshToken;
import org.lucky0111.pettalkmcpserver.domain.entity.user.PetUser;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface RefreshTokenRepository extends JpaRepository<RefreshToken, String> {

    Optional<RefreshToken> findByToken(String token);

    List<RefreshToken> findAllByUser(PetUser user);

    @Modifying
    @Query("UPDATE RefreshToken r SET r.revoked = true WHERE r.user.userId = :userId AND r.revoked = false")
    void revokeAllByUser(UUID userId);

    @Modifying
    @Query("DELETE FROM RefreshToken r WHERE r.expiryDate < :now")
    void deleteAllExpiredTokens(LocalDateTime now);
}