package com.smartfactory.scada.user.domain;

import java.time.LocalDateTime;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class User {

	private Long id;

	private String email;

	private String passwordHash;

	private String name;

	private String phone;

	private UserRole role;

	private Long plantId;

	private UserStatus status;

	private String note;

	private LocalDateTime lastLoginAt;

	private LocalDateTime createdAt;

	private LocalDateTime updatedAt;
}
