package com.corpoviva.vivent.security;

import com.corpoviva.vivent.common.exception.ErrorCode;
import com.corpoviva.vivent.common.response.ApiResponse;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import jakarta.servlet.http.HttpServletResponse;

@Component
public class SecurityResponseWriter {

	private final ObjectMapper objectMapper;

	public SecurityResponseWriter(ObjectMapper objectMapper) {
		this.objectMapper = objectMapper;
	}

	public void write(HttpServletResponse response, HttpStatus status, String message, ErrorCode errorCode)
		throws IOException {
		response.setStatus(status.value());
		response.setContentType(MediaType.APPLICATION_JSON_VALUE);
		objectMapper.writeValue(response.getOutputStream(), ApiResponse.failure(message, errorCode));
	}
}
