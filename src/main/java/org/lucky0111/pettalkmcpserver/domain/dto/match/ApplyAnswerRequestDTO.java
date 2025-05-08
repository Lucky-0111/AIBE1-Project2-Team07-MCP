package org.lucky0111.pettalkmcpserver.domain.dto.match;

import org.lucky0111.pettalkmcpserver.domain.common.ApplyReason;
import org.lucky0111.pettalkmcpserver.domain.common.ApplyStatus;

public record ApplyAnswerRequestDTO(
        Long applyId,
        ApplyStatus applyStatus,
        String content,
        ApplyReason applyReason
) {}
