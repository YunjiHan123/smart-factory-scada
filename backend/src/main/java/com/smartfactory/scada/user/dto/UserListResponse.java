package com.smartfactory.scada.user.dto;

import java.util.List;

import io.swagger.v3.oas.annotations.media.Schema;

public record UserListResponse(
	@Schema(description = "사용자 목록")
	List<UserListItemResponse> items,

	@Schema(description = "현재 페이지 번호. 0부터 시작합니다.", example = "0")
	int page,

	@Schema(description = "페이지 크기", example = "20")
	int size,

	@Schema(description = "전체 사용자 수", example = "57")
	long totalCount,

	@Schema(description = "전체 페이지 수", example = "3")
	int totalPages
) {

	public static UserListResponse of(List<UserListItemResponse> items, UserListRequest request, long totalCount) {
		return new UserListResponse(
			items,
			request.page(),
			request.size(),
			totalCount,
			calculateTotalPages(totalCount, request.size())
		);
	}

	private static int calculateTotalPages(long totalCount, int size) {
		if (totalCount == 0) {
			return 0;
		}

		return (int)Math.ceil((double)totalCount / size);
	}
}
