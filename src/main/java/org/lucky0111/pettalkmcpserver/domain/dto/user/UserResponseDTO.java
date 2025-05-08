package org.lucky0111.pettalkmcpserver.domain.dto.user;

import org.lucky0111.pettalkmcpserver.domain.common.UserRole;
import org.lucky0111.pettalkmcpserver.domain.entity.user.PetUser;

public record UserResponseDTO(String name, String nickname, String email, String profileImageUrl, UserRole userRole) {

    public static UserResponseDTO from(PetUser petUser) {
        return new UserResponseDTO(petUser.getName(), petUser.getNickname(), petUser.getEmail(), petUser.getProfileImageUrl(), petUser.getRole());
    }
}
