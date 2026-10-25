package com.corpoviva.vivent.sales.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record UpsertCustomerRequest(
	@NotBlank @Size(max = 150) String name,
	@Size(max = 50) String taxId,
	@Email @Size(max = 150) String email,
	@Size(max = 50) String phone,
	@Size(max = 255) String address,
	boolean genericConsumer,
	boolean active
) {
}
