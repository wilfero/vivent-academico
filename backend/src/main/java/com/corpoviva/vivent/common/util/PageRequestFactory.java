package com.corpoviva.vivent.common.util;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

public final class PageRequestFactory {

	private static final int MIN_PAGE = 1;
	private static final int DEFAULT_PAGE_SIZE = 20;
	private static final int MAX_PAGE_SIZE = 100;

	private PageRequestFactory() {
	}

	public static Pageable create(int page, int pageSize, Sort sort) {
		int normalizedPage = Math.max(page, MIN_PAGE);
		int normalizedPageSize = normalizePageSize(pageSize);
		return PageRequest.of(normalizedPage - 1, normalizedPageSize, sort);
	}

	public static int normalizePage(int page) {
		return Math.max(page, MIN_PAGE);
	}

	public static int normalizePageSize(int pageSize) {
		if (pageSize <= 0) {
			return DEFAULT_PAGE_SIZE;
		}

		return Math.min(pageSize, MAX_PAGE_SIZE);
	}
}
