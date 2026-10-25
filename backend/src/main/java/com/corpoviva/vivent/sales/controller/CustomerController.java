package com.corpoviva.vivent.sales.controller;

import com.corpoviva.vivent.common.dto.PageResponse;
import com.corpoviva.vivent.common.response.ApiResponse;
import com.corpoviva.vivent.sales.dto.CustomerResponse;
import com.corpoviva.vivent.sales.dto.UpsertCustomerRequest;
import com.corpoviva.vivent.sales.service.CustomerService;
import com.corpoviva.vivent.security.AuthenticatedUser;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import java.util.UUID;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Validated
@RestController
@RequestMapping("/api/v1/customers")
public class CustomerController {

	private final CustomerService customerService;

	public CustomerController(CustomerService customerService) {
		this.customerService = customerService;
	}

	@GetMapping
	@PreAuthorize("hasAuthority('customers.view')")
	public ApiResponse<PageResponse<CustomerResponse>> listCustomers(
		@AuthenticationPrincipal AuthenticatedUser user,
		@RequestParam(defaultValue = "1") @Min(1) int page,
		@RequestParam(defaultValue = "20") @Min(1) @Max(100) int pageSize,
		@RequestParam(required = false) String query,
		@RequestParam(required = false) Boolean active
	) {
		return ApiResponse.success(customerService.listCustomers(user, page, pageSize, query, active));
	}

	@GetMapping("/{customerId}")
	@PreAuthorize("hasAuthority('customers.view')")
	public ApiResponse<CustomerResponse> getCustomer(@AuthenticationPrincipal AuthenticatedUser user, @PathVariable UUID customerId) {
		return ApiResponse.success(customerService.getCustomer(user, customerId));
	}

	@PostMapping
	@PreAuthorize("hasAuthority('customers.create')")
	public ApiResponse<CustomerResponse> createCustomer(
		@AuthenticationPrincipal AuthenticatedUser user,
		@Valid @RequestBody UpsertCustomerRequest request
	) {
		return ApiResponse.success(customerService.createCustomer(user, request));
	}

	@PutMapping("/{customerId}")
	@PreAuthorize("hasAuthority('customers.update')")
	public ApiResponse<CustomerResponse> updateCustomer(
		@AuthenticationPrincipal AuthenticatedUser user,
		@PathVariable UUID customerId,
		@Valid @RequestBody UpsertCustomerRequest request
	) {
		return ApiResponse.success(customerService.updateCustomer(user, customerId, request));
	}

	@DeleteMapping("/{customerId}")
	@PreAuthorize("hasAuthority('customers.delete')")
	public ApiResponse<Void> deleteCustomer(@AuthenticationPrincipal AuthenticatedUser user, @PathVariable UUID customerId) {
		customerService.deleteCustomer(user, customerId);
		return ApiResponse.success(null);
	}
}
