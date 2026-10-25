package com.corpoviva.vivent.users.dto;

import jakarta.validation.constraints.NotNull;
import java.util.UUID;

public record ChangeUserBranchRequest(@NotNull UUID branchId) {
}
