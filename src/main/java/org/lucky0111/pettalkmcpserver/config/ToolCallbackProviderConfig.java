package org.lucky0111.pettalkmcpserver.config;

import lombok.RequiredArgsConstructor;
import org.lucky0111.pettalkmcpserver.service.ChatService;
import org.lucky0111.pettalkmcpserver.service.TagService;
import org.springframework.ai.tool.ToolCallbackProvider;
import org.springframework.ai.tool.method.MethodToolCallbackProvider;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@RequiredArgsConstructor
public class ToolCallbackProviderConfig {
    private final TagService tagService;
    private final ChatService chatService;

    @Bean
    public ToolCallbackProvider toolCallbackProvider() {
        return MethodToolCallbackProvider.builder()
                .toolObjects(
                        tagService,
                        chatService
                )
                .build();
    }
}
