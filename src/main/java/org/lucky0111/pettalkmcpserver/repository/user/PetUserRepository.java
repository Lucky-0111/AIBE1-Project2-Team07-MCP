package org.lucky0111.pettalkmcpserver.repository.user;


import org.lucky0111.pettalkmcpserver.domain.common.UserRole;
import org.lucky0111.pettalkmcpserver.domain.entity.user.PetUser;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface PetUserRepository extends JpaRepository<PetUser, UUID> {
    Optional<PetUser> findByEmail(String email);
    Optional<PetUser> findByNickname(String nickname);
    Optional<PetUser> findBySocialId(String socialId);

    PetUser findByProviderAndSocialId(String provider, String socialId);
    List<PetUser> findByName(String name);

    boolean existsByNickname(String nickname);

    Optional<PetUser> findAllByProvider(String provider);

    List<PetUser> findByNameContainingAndRole(String name, UserRole role);

    List<PetUser> findByNicknameContainingAndRole(String nickname, UserRole role);
}
