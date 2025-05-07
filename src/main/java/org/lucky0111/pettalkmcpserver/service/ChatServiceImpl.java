package org.lucky0111.pettalkmcpserver.service;

import lombok.RequiredArgsConstructor;
import org.lucky0111.pettalkmcpserver.domain.entity.user.PetUser;
import org.lucky0111.pettalkmcpserver.repository.user.PetUserRepository;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ChatServiceImpl implements ChatService {
    private final PetUserRepository petUserRepository;

    @Tool(name = "getUserNicknameForTest", description = "모든 특정 서비스 로그인 유저를 찾고, 유저의 닉네임을 반환합니다. 특정 서비스 로그인 유저의 닉네임이 필요할 때 이 도구를 사용하세요. 예: '네이버 로그인 유저의 닉네임을 조회해줘.', '카카오 유저의 닉네임을 알려줘.'")
    public List<String> getUserNicknameForTest(@ToolParam(description = "소셜 로그인 서비스 제공자. 예: '네이버', '카카오'") String provider) {
        return petUserRepository.findAllByProvider(provider)
                .stream()
                .map(PetUser::getNickname)
                .toList();
    }

    @Tool(name = "test", description = "내 이름이 무엇인지 물어볼 때 사용하세요. 예: '내 이름이 뭐야?', '내 이름은?', '내 이름은 무엇인가요?', '내 이름은 무엇입니까?', '내 이름은?'")
    public String test() {return "강평종";}
}
