package com.smartfactory.scada.auth.security;

public record AuthenticatedUser(
	Long userId,
	String email,
	String name,
	String phone,
	String role,
	Long plantId,
	String status
) {
}
