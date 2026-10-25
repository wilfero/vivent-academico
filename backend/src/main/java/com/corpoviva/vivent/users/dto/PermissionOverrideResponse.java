package com.corpoviva.vivent.users.dto;

import com.corpoviva.vivent.users.entity.PermissionEffect;

public record PermissionOverrideResponse(String permissionCode, PermissionEffect effect) {
}
