package com.corpoviva.vivent.permissions.service;

import com.corpoviva.vivent.users.entity.PermissionEffect;
import com.corpoviva.vivent.users.entity.UserEntity;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.TreeSet;
import org.springframework.stereotype.Service;

@Service
public class PermissionResolutionService {

	public Set<String> resolvePermissions(UserEntity user) {
		Set<String> permissions = user.getRole()
			.getPermissions()
			.stream()
			.map(permission -> permission.getCode())
			.collect(java.util.stream.Collectors.toCollection(TreeSet::new));

		user.getPermissionOverrides()
			.stream()
			.filter(override -> override.getEffect() == PermissionEffect.DENY)
			.map(override -> override.getPermission().getCode())
			.forEach(permissions::remove);

		user.getPermissionOverrides()
			.stream()
			.filter(override -> override.getEffect() == PermissionEffect.GRANT)
			.map(override -> override.getPermission().getCode())
			.forEach(permissions::add);

		return Set.copyOf(new LinkedHashSet<>(permissions));
	}
}
