package com.corpoviva.vivent.common.dto;

import java.util.List;
import org.springframework.data.domain.Page;

public record PageResponse<T>(
	List<T> items,
	int page,
	int pageSize,
	long totalItems,
	int totalPages
) {

	public static <T> PageResponse<T> from(Page<T> pageData, int requestedPage, int requestedPageSize) {
		return new PageResponse<>(
			pageData.getContent(),
			requestedPage,
			requestedPageSize,
			pageData.getTotalElements(),
			pageData.getTotalPages()
		);
	}
}
