package org.lucky0111.pettalkmcpserver.domain.dto.review;

public record ReviewUpdateDTO(
        Integer rating,
        String title,
        String comment,
        String reviewImageUrl
) {
}
