package com.corpoviva.vivent.products.dto;

import com.corpoviva.vivent.products.entity.CategoryEntity;
import java.util.UUID;

public record CategoryResponse(
	UUID id,
	UUID companyId,
	UUID parentCategoryId,
	String name,
	String description,
	boolean active
) {

	public static CategoryResponse from(CategoryEntity category) {
		UUID parentId = category.getParentCategory() == null ? null : category.getParentCategory().getId();
		return new CategoryResponse(
			category.getId(),
			category.getCompany().getId(),
			parentId,
			category.getName(),
			category.getDescription(),
			category.isActive()
		);
	}
}
