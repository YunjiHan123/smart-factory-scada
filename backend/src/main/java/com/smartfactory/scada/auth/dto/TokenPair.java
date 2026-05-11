package com.smartfactory.scada.auth.dto;

public record TokenPair(
	String accessToken,
	String refreshToken
) {
}
