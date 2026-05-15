package com.smartfactory.scada.user.dto;

import org.springframework.util.StringUtils;

import com.smartfactory.scada.user.domain.UserRole;
import com.smartfactory.scada.user.domain.UserStatus;

public record UserListRequest(
	int page,
	int size,
	int offset,
	String keyword,
	UserRole role,
	UserStatus status,
	Long plantId
) {

	private static final int DEFAULT_PAGE = 0;
	private static final int DEFAULT_SIZE = 20;
	private static final int MAX_SIZE = 100;

	public static UserListRequest of(
		Integer page,
		Integer size,
		String keyword,
		UserRole role,
		UserStatus status,
		Long plantId
	) {
		int normalizedPage = normalizePage(page);
		int normalizedSize = normalizeSize(size);

		return new UserListRequest(
			normalizedPage,
			normalizedSize,
			normalizedPage * normalizedSize,
			normalizeKeyword(keyword),
			role,
			status,
			plantId
		);
	}

	private static int normalizePage(Integer page) {
		if (page == null || page < 0) {
			return DEFAULT_PAGE;
		}

		return page;
	}

	private static int normalizeSize(Integer size) {
		if (size == null || size < 1) {
			return DEFAULT_SIZE;
		}

		return Math.min(size, MAX_SIZE);
	}

	private static String normalizeKeyword(String keyword) {
		if (!StringUtils.hasText(keyword)) {
			return null;
		}

		return keyword.trim();
	}
}
