package org.lucky0111.pettalkmcpserver.domain.dto.user;

import java.util.UUID;

public record UserDTO (String role, String name, String provider, String socialId, UUID userId, String email) {

}
