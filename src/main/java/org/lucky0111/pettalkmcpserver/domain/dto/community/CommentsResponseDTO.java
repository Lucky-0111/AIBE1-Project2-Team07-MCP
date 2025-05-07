package org.lucky0111.pettalkmcpserver.domain.dto.community;

import java.util.List;

public record CommentsResponseDTO(
        List<CommentResponseDTO> comments,
        Long nextCursor,
        boolean hasMore
) {}
