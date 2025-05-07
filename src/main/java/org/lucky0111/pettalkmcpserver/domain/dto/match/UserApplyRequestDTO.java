package org.lucky0111.pettalkmcpserver.domain.dto.match;

public record UserApplyRequestDTO(
        String trainerName,
        String petType,
        String petBreed,
        Integer petMonthAge,
        String content,
        String imageUrl
) {
}
