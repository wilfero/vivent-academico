package com.corpoviva.vivent.common.response;

import com.corpoviva.vivent.common.exception.ErrorCode;

public record ApiResponse<T>(boolean success, T data, String message, String errorCode) {

	public static <T> ApiResponse<T> success(T data) {
		return new ApiResponse<>(true, data, null, null);
	}

	public static ApiResponse<Void> failure(String message, ErrorCode errorCode) {
		return new ApiResponse<>(false, null, message, errorCode.name());
	}
}
