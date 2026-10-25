package com.corpoviva.vivent.purchases.dto;

import com.corpoviva.vivent.purchases.entity.SupplierEntity;
import java.util.UUID;

public record SupplierResponse(
	UUID id,
	UUID companyId,
	String name,
	String taxId,
	String email,
	String phone,
	String address,
	String contactName,
	boolean active
) {

	public static SupplierResponse from(SupplierEntity supplier) {
		return new SupplierResponse(
			supplier.getId(),
			supplier.getCompany().getId(),
			supplier.getName(),
			supplier.getTaxId(),
			supplier.getEmail(),
			supplier.getPhone(),
			supplier.getAddress(),
			supplier.getContactName(),
			supplier.isActive()
		);
	}
}
