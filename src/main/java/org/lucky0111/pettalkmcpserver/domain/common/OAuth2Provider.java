package org.lucky0111.pettalkmcpserver.domain.common;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum OAuth2Provider {
    KAKAO("kakao"),
    NAVER("naver");

    private final String registrationId;
}
