package org.lucky0111.pettalkmcpserver.domain.dto.auth;

public record TokenDTO(String accessToken, String refreshToken, long expiresIn) {

}