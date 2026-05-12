package com.smartfactory.scada.auth.security;

public record AuthenticatedUser(
	Long userId,
	String email,
	String nickname
) {
}
