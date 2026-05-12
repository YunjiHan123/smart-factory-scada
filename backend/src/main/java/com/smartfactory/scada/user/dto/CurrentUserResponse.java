package com.smartfactory.scada.user.dto;

public record CurrentUserResponse(
	Long userId,
	String email,
	String nickname
) {
}
