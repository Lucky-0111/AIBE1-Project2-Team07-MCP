package org.lucky0111.pettalkmcpserver.domain.dto.community;

public record CommentRequestDTO(
        Long postId,
        Long parentCommentId,
        String content
) {}
