package org.lucky0111.pettalkmcpserver.domain.dto.review;

public record ReviewRequestDTO(
        Long applyId,
        Integer rating,
        String title,
        String comment,
        String reviewImageUrl
) {
}
