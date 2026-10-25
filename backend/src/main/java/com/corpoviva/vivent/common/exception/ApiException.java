package com.corpoviva.vivent.common.exception;

import org.springframework.http.HttpStatus;

public class ApiException extends RuntimeException {

	private final HttpStatus status;
	private final ErrorCode errorCode;

	public ApiException(HttpStatus status, ErrorCode errorCode, String message) {
		super(message);
		this.status = status;
		this.errorCode = errorCode;
	}

	public HttpStatus getStatus() {
		return status;
	}

	public ErrorCode getErrorCode() {
		return errorCode;
	}

	public static ApiException badRequest(ErrorCode errorCode, String message) {
		return new ApiException(HttpStatus.BAD_REQUEST, errorCode, message);
	}

	public static ApiException unauthorized(ErrorCode errorCode, String message) {
		return new ApiException(HttpStatus.UNAUTHORIZED, errorCode, message);
	}

	public static ApiException forbidden(ErrorCode errorCode, String message) {
		return new ApiException(HttpStatus.FORBIDDEN, errorCode, message);
	}

	public static ApiException notFound(ErrorCode errorCode, String message) {
		return new ApiException(HttpStatus.NOT_FOUND, errorCode, message);
	}
}
