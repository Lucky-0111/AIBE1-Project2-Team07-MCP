package org.lucky0111.pettalkmcpserver.domain.dto.community;

public record PostLikeResponseDTO(
        Long likeId,
        Long postId,
        Boolean liked
) {}
