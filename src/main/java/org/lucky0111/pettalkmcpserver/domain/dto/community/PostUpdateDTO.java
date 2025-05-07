package org.lucky0111.pettalkmcpserver.domain.dto.community;

import org.lucky0111.pettalkmcpserver.domain.common.PetCategory;
import org.lucky0111.pettalkmcpserver.domain.common.PostCategory;

import java.util.List;

public record PostUpdateDTO(
        PostCategory postCategory,
        PetCategory petCategory,
        String title,
        String content,
        String imageUrl,
        String videoUrl,
        List<Long> tagIds
) {}