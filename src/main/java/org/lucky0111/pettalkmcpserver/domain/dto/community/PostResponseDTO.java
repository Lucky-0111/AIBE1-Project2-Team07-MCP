package org.lucky0111.pettalkmcpserver.domain.dto.community;

import org.lucky0111.pettalkmcpserver.domain.common.PetCategory;
import org.lucky0111.pettalkmcpserver.domain.common.PostCategory;

import java.util.List;

public record PostResponseDTO(
        Long postId,
        String userName,
        String userNickname,
        String profileImageUrl,
        PostCategory postCategory,
        PetCategory petCategory,
        String title,
        String content,
        List<String> imageUrls,
        String videoUrl,
        Integer likeCount,
        Integer commentCount,
        Boolean hasLiked,
        List<String> tags,
        String createdAt,
        String updatedAt
) {}
