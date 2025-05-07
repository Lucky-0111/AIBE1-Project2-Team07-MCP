package org.lucky0111.pettalkmcpserver.domain.dto.match;

import org.lucky0111.pettalkmcpserver.domain.common.ApplyStatus;

public record UserApplyResponseDTO(
        Long applyId,
        String userName,
        String trainerName,
        String petType,
        String petBreed,
        Integer petMonthAge,
        String content,
        String imageUrl,
        ApplyStatus applyStatus,
        String createdAt,
        String updatedAt
){}
