package org.lucky0111.pettalkmcpserver.domain.dto.admin;

import org.lucky0111.pettalkmcpserver.domain.common.UserRole;

import java.util.UUID;

public record AdminUserDTO(UUID userId, String name, String nickname, String email, String profileImageUrl, UserRole role, String provider, String socialId, String status) {
}
