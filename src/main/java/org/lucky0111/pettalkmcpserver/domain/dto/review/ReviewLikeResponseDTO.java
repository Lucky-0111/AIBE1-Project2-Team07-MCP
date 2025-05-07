package org.lucky0111.pettalkmcpserver.domain.dto.review;

import java.util.UUID;

public record ReviewLikeResponseDTO(
        Long likeId,
        Long reviewId,
        UUID userId,
        String createdAt
) {
}
