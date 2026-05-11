package com.smartfactory.scada.auth.dto;

public record SignupResponse(
	Long userId,
	String email,
	String nickname
) {
}
