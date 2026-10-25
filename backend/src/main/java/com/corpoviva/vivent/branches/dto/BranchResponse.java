package com.corpoviva.vivent.branches.dto;

import com.corpoviva.vivent.branches.entity.BranchEntity;
import java.util.UUID;

public record BranchResponse(
	UUID id,
	UUID companyId,
	String name,
	String code,
	String address,
	String phone,
	String email,
	boolean active
) {

	public static BranchResponse from(BranchEntity branch) {
		return new BranchResponse(
			branch.getId(),
			branch.getCompany().getId(),
			branch.getName(),
			branch.getCode(),
			branch.getAddress(),
			branch.getPhone(),
			branch.getEmail(),
			branch.isActive()
		);
	}
}
