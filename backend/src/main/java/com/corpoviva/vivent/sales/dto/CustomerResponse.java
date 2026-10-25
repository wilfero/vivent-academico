package com.corpoviva.vivent.sales.dto;

import com.corpoviva.vivent.sales.entity.CustomerEntity;
import java.util.UUID;

public record CustomerResponse(
	UUID id,
	UUID companyId,
	String name,
	String taxId,
	String email,
	String phone,
	String address,
	boolean genericConsumer,
	boolean active
) {

	public static CustomerResponse from(CustomerEntity customer) {
		return new CustomerResponse(
			customer.getId(),
			customer.getCompany().getId(),
			customer.getName(),
			customer.getTaxId(),
			customer.getEmail(),
			customer.getPhone(),
			customer.getAddress(),
			customer.isGenericConsumer(),
			customer.isActive()
		);
	}
}
