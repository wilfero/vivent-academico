package com.corpoviva.vivent.common.exception;

import com.corpoviva.vivent.common.response.ApiResponse;
import com.corpoviva.vivent.security.MissingPermissionResolver;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolationException;
import java.util.List;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalExceptionHandler {

	private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);
	private final MissingPermissionResolver missingPermissionResolver;

	public GlobalExceptionHandler(MissingPermissionResolver missingPermissionResolver) {
		this.missingPermissionResolver = missingPermissionResolver;
	}

	@ExceptionHandler(ApiException.class)
	public ResponseEntity<ApiResponse<Void>> handleApiException(ApiException exception) {
		return ResponseEntity.status(exception.getStatus())
			.body(ApiResponse.failure(exception.getMessage(), exception.getErrorCode()));
	}

	@ExceptionHandler(AccessDeniedException.class)
	public ResponseEntity<ApiResponse<Void>> handleAccessDeniedException(
		AccessDeniedException exception,
		HttpServletRequest request
	) {
		List<String> missingPermissions = missingPermissionResolver.resolve(request);
		String message = missingPermissions.isEmpty()
			? "Permission denied"
			: missingPermissionResolver.formatMessage(missingPermissions);

		return ResponseEntity.status(HttpStatus.FORBIDDEN)
			.body(ApiResponse.failure(message, ErrorCode.PERMISSION_DENIED));
	}

	@ExceptionHandler(MethodArgumentNotValidException.class)
	public ResponseEntity<ApiResponse<Void>> handleValidationException(MethodArgumentNotValidException exception) {
		String message = exception.getBindingResult()
			.getFieldErrors()
			.stream()
			.map(this::formatFieldError)
			.collect(Collectors.joining(", "));

		return ResponseEntity.badRequest().body(ApiResponse.failure(message, ErrorCode.VALIDATION_ERROR));
	}

	@ExceptionHandler({ConstraintViolationException.class, MissingServletRequestParameterException.class,
		HttpMessageNotReadableException.class})
	public ResponseEntity<ApiResponse<Void>> handleBadRequest(Exception exception) {
		return ResponseEntity.badRequest()
			.body(ApiResponse.failure(exception.getMessage(), ErrorCode.VALIDATION_ERROR));
	}

	@ExceptionHandler(Exception.class)
	public ResponseEntity<ApiResponse<Void>> handleUnexpectedException(Exception exception) {
		log.error("Unhandled exception", exception);
		return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
			.body(ApiResponse.failure("Unexpected server error", ErrorCode.VALIDATION_ERROR));
	}

	private String formatFieldError(FieldError fieldError) {
		return fieldError.getField() + ": " + fieldError.getDefaultMessage();
	}
}
