package com.corpoviva.vivent.users.service;

import com.corpoviva.vivent.branches.entity.BranchEntity;
import com.corpoviva.vivent.branches.repository.BranchRepository;
import com.corpoviva.vivent.common.dto.PageResponse;
import com.corpoviva.vivent.common.exception.ApiException;
import com.corpoviva.vivent.common.exception.ErrorCode;
import com.corpoviva.vivent.common.util.PageRequestFactory;
import com.corpoviva.vivent.companies.entity.CompanyEntity;
import com.corpoviva.vivent.companies.repository.CompanyRepository;
import com.corpoviva.vivent.permissions.entity.PermissionEntity;
import com.corpoviva.vivent.permissions.entity.RoleEntity;
import com.corpoviva.vivent.permissions.repository.RoleRepository;
import com.corpoviva.vivent.permissions.service.PermissionCatalogService;
import com.corpoviva.vivent.permissions.service.PermissionResolutionService;
import com.corpoviva.vivent.security.AuthenticatedUser;
import com.corpoviva.vivent.subscriptions.service.SubscriptionPolicyService;
import com.corpoviva.vivent.users.dto.ChangeUserBranchRequest;
import com.corpoviva.vivent.users.dto.CreateUserRequest;
import com.corpoviva.vivent.users.dto.PermissionOverrideRequest;
import com.corpoviva.vivent.users.dto.ResetUserPasswordRequest;
import com.corpoviva.vivent.users.dto.UpdateUserRequest;
import com.corpoviva.vivent.users.dto.UserResponse;
import com.corpoviva.vivent.users.entity.PermissionEffect;
import com.corpoviva.vivent.users.entity.UserBranchAssignmentEntity;
import com.corpoviva.vivent.users.entity.UserBranchAssignmentStatus;
import com.corpoviva.vivent.users.entity.UserEntity;
import com.corpoviva.vivent.users.entity.UserPermissionOverrideEntity;
import com.corpoviva.vivent.users.entity.UserStatus;
import com.corpoviva.vivent.users.repository.UserBranchAssignmentRepository;
import com.corpoviva.vivent.users.repository.UserRepository;
import java.time.Instant;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Sort;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class UserService {
	private static final String COMPANY_ADMIN = "COMPANY_ADMIN";

	private final UserRepository userRepository;
	private final BranchRepository branchRepository;
	private final CompanyRepository companyRepository;
	private final RoleRepository roleRepository;
	private final PermissionCatalogService permissionCatalogService;
	private final PermissionResolutionService permissionResolutionService;
	private final UserBranchAssignmentRepository userBranchAssignmentRepository;
	private final SubscriptionPolicyService subscriptionPolicyService;
	private final PasswordEncoder passwordEncoder;

	public UserService(
		UserRepository userRepository,
		BranchRepository branchRepository,
		CompanyRepository companyRepository,
		RoleRepository roleRepository,
		PermissionCatalogService permissionCatalogService,
		PermissionResolutionService permissionResolutionService,
		UserBranchAssignmentRepository userBranchAssignmentRepository,
		SubscriptionPolicyService subscriptionPolicyService,
		PasswordEncoder passwordEncoder
	) {
		this.userRepository = userRepository;
		this.branchRepository = branchRepository;
		this.companyRepository = companyRepository;
		this.roleRepository = roleRepository;
		this.permissionCatalogService = permissionCatalogService;
		this.permissionResolutionService = permissionResolutionService;
		this.userBranchAssignmentRepository = userBranchAssignmentRepository;
		this.subscriptionPolicyService = subscriptionPolicyService;
		this.passwordEncoder = passwordEncoder;
	}

	@Transactional(readOnly = true)
	public PageResponse<UserResponse> listUsers(
		AuthenticatedUser authenticatedUser,
		int page,
		int pageSize,
		String query,
		UserStatus status,
		UUID branchId
	) {
		int normalizedPage = PageRequestFactory.normalizePage(page);
		int normalizedPageSize = PageRequestFactory.normalizePageSize(pageSize);
		UUID scopedBranchId = scopedBranchId(authenticatedUser, branchId);
		Page<UserEntity> userPage = userRepository.searchByCompany(
			authenticatedUser.companyId(),
			normalizeQuery(query),
			status,
			scopedBranchId,
			PageRequestFactory.create(normalizedPage, normalizedPageSize, Sort.by("username").ascending())
		);

		Page<UserResponse> responsePage = userPage.map(user -> UserResponse.from(user, permissionResolutionService.resolvePermissions(user)));
		return PageResponse.from(responsePage, normalizedPage, normalizedPageSize);
	}

	@Transactional(readOnly = true)
	public UserResponse getUser(AuthenticatedUser authenticatedUser, UUID userId) {
		UserEntity user = loadUser(authenticatedUser, userId);
		return UserResponse.from(user, permissionResolutionService.resolvePermissions(user));
	}

	@Transactional
	public UserResponse createUser(AuthenticatedUser authenticatedUser, CreateUserRequest request) {
		CompanyEntity company = loadCompany(authenticatedUser.companyId());
		String normalizedUsername = normalizeUsername(request.username());
		ensureUsernameAvailable(company.getId(), normalizedUsername, null);

		BranchEntity branch = loadActiveBranch(company.getId(), request.branchId());
		RoleEntity role = loadAccessibleRole(company.getId(), request.roleId());
		UserStatus status = request.status() == null ? UserStatus.ACTIVE : request.status();

		if (status == UserStatus.DELETED) {
			throw ApiException.badRequest(ErrorCode.VALIDATION_ERROR, "A new user cannot be created with DELETED status");
		}

		if (status == UserStatus.ACTIVE) {
			subscriptionPolicyService.ensureUserCapacity(company.getId());
		}

		UserEntity user = new UserEntity();
		ensureCanAccessBranch(authenticatedUser, branch.getId());
		user.setCompany(company);
		user.setBranch(branch);
		user.setRole(role);
		user.setUsername(normalizedUsername);
		user.setPasswordHash(passwordEncoder.encode(request.password()));
		user.setFirstName(request.firstName().trim());
		user.setLastName(request.lastName().trim());
		user.setEmail(request.email().trim().toLowerCase(Locale.ROOT));
		user.setPhone(trimToNull(request.phone()));
		user.setBirthDate(request.birthDate());
		user.setStatus(status);
		applyPermissionOverrides(user, request.permissionOverrides());

		UserEntity savedUser = userRepository.save(user);
		createPrimaryBranchAssignment(savedUser, branch);

		return UserResponse.from(savedUser, permissionResolutionService.resolvePermissions(savedUser));
	}

	@Transactional
	public UserResponse updateUser(AuthenticatedUser authenticatedUser, UUID userId, UpdateUserRequest request) {
		UserEntity user = loadUser(authenticatedUser, userId);
		String normalizedUsername = normalizeUsername(request.username());

		ensureUserNotDeleted(user);
		ensureUsernameAvailable(authenticatedUser.companyId(), normalizedUsername, user.getId());

		RoleEntity role = loadAccessibleRole(authenticatedUser.companyId(), request.roleId());

		user.setUsername(normalizedUsername);
		user.setFirstName(request.firstName().trim());
		user.setLastName(request.lastName().trim());
		user.setEmail(request.email().trim().toLowerCase(Locale.ROOT));
		user.setPhone(trimToNull(request.phone()));
		user.setBirthDate(request.birthDate());
		user.setRole(role);

		if (request.password() != null && !request.password().isBlank()) {
			user.setPasswordHash(passwordEncoder.encode(request.password()));
			user.setAuthVersion(user.getAuthVersion() + 1);
		}

		applyPermissionOverrides(user, request.permissionOverrides());

		return UserResponse.from(user, permissionResolutionService.resolvePermissions(user));
	}

	@Transactional
	public UserResponse activateUser(AuthenticatedUser authenticatedUser, UUID userId) {
		UserEntity user = loadUser(authenticatedUser, userId);
		ensureUserNotDeleted(user);

		if (!user.getBranch().isActive()) {
			throw ApiException.badRequest(ErrorCode.VALIDATION_ERROR, "User branch must be active before activation");
		}

		if (user.getStatus() != UserStatus.ACTIVE) {
			subscriptionPolicyService.ensureUserCapacity(authenticatedUser.companyId());
			user.setStatus(UserStatus.ACTIVE);
		}

		return UserResponse.from(user, permissionResolutionService.resolvePermissions(user));
	}

	@Transactional
	public UserResponse deactivateUser(AuthenticatedUser authenticatedUser, UUID userId) {
		preventSelfMutation(authenticatedUser, userId, "You cannot deactivate your own account");
		UserEntity user = loadUser(authenticatedUser, userId);
		ensureUserNotDeleted(user);
		user.setStatus(UserStatus.INACTIVE);
		return UserResponse.from(user, permissionResolutionService.resolvePermissions(user));
	}

	@Transactional
	public UserResponse changeUserBranch(AuthenticatedUser authenticatedUser, UUID userId, ChangeUserBranchRequest request) {
		UserEntity user = loadUser(authenticatedUser, userId);
		ensureUserNotDeleted(user);

		BranchEntity branch = loadActiveBranch(authenticatedUser.companyId(), request.branchId());
		ensureCanAccessBranch(authenticatedUser, branch.getId());
		user.setBranch(branch);
		rotatePrimaryBranchAssignment(user, branch);

		return UserResponse.from(user, permissionResolutionService.resolvePermissions(user));
	}

	@Transactional
	public UserResponse resetUserPassword(AuthenticatedUser authenticatedUser, UUID userId, ResetUserPasswordRequest request) {
		UserEntity user = loadUser(authenticatedUser, userId);
		ensureUserNotDeleted(user);
		user.setPasswordHash(passwordEncoder.encode(request.password()));
		user.setAuthVersion(user.getAuthVersion() + 1);
		return UserResponse.from(user, permissionResolutionService.resolvePermissions(user));
	}

	@Transactional
	public void deleteUser(AuthenticatedUser authenticatedUser, UUID userId) {
		preventSelfMutation(authenticatedUser, userId, "You cannot delete your own account");
		UserEntity user = loadUser(authenticatedUser, userId);
		ensureUserNotDeleted(user);

		user.setStatus(UserStatus.DELETED);
		user.setDeletedAt(Instant.now());
		deactivateAssignments(user);
	}

	private void applyPermissionOverrides(UserEntity user, Collection<PermissionOverrideRequest> requests) {
		user.getPermissionOverrides().clear();

		if (requests == null || requests.isEmpty()) {
			return;
		}

		Map<String, PermissionEffect> effectByPermissionCode = new LinkedHashMap<>();
		for (PermissionOverrideRequest request : requests) {
			effectByPermissionCode.put(request.permissionCode().trim(), request.effect());
		}

		Set<PermissionEntity> permissions = permissionCatalogService.resolvePermissionEntities(effectByPermissionCode.keySet());
		for (PermissionEntity permission : permissions) {
			UserPermissionOverrideEntity override = new UserPermissionOverrideEntity();
			override.setUser(user);
			override.setPermission(permission);
			override.setEffect(effectByPermissionCode.get(permission.getCode()));
			user.getPermissionOverrides().add(override);
		}
	}

	private void createPrimaryBranchAssignment(UserEntity user, BranchEntity branch) {
		UserBranchAssignmentEntity assignment = new UserBranchAssignmentEntity();
		assignment.setUser(user);
		assignment.setBranch(branch);
		assignment.setPrimary(true);
		assignment.setStatus(UserBranchAssignmentStatus.ACTIVE);
		assignment.setStartAt(Instant.now());
		userBranchAssignmentRepository.save(assignment);
	}

	private void rotatePrimaryBranchAssignment(UserEntity user, BranchEntity branch) {
		Instant now = Instant.now();
		List<UserBranchAssignmentEntity> assignments = userBranchAssignmentRepository.findByUser_Id(user.getId());
		UserBranchAssignmentEntity targetAssignment = null;

		for (UserBranchAssignmentEntity assignment : assignments) {
			if (assignment.getBranch().getId().equals(branch.getId())) {
				targetAssignment = assignment;
			}
			if (assignment.getStatus() == UserBranchAssignmentStatus.ACTIVE) {
				assignment.setStatus(UserBranchAssignmentStatus.INACTIVE);
				assignment.setPrimary(false);
				assignment.setEndAt(now);
			}
		}
		userBranchAssignmentRepository.flush();

		if (targetAssignment == null) {
			targetAssignment = new UserBranchAssignmentEntity();
			targetAssignment.setUser(user);
			targetAssignment.setBranch(branch);
		}

		targetAssignment.setPrimary(true);
		targetAssignment.setStatus(UserBranchAssignmentStatus.ACTIVE);
		targetAssignment.setStartAt(now);
		targetAssignment.setEndAt(null);
		userBranchAssignmentRepository.save(targetAssignment);
	}

	private void deactivateAssignments(UserEntity user) {
		Instant now = Instant.now();
		List<UserBranchAssignmentEntity> assignments = userBranchAssignmentRepository.findByUser_Id(user.getId());
		for (UserBranchAssignmentEntity assignment : assignments) {
			assignment.setStatus(UserBranchAssignmentStatus.INACTIVE);
			assignment.setPrimary(false);
			if (assignment.getEndAt() == null) {
				assignment.setEndAt(now);
			}
		}
	}

	private CompanyEntity loadCompany(UUID companyId) {
		return companyRepository.findByIdAndDeletedAtIsNull(companyId)
			.orElseThrow(() -> ApiException.notFound(ErrorCode.TENANT_NOT_FOUND, "Company not found"));
	}

	private UserEntity loadUser(UUID companyId, UUID userId) {
		return userRepository.findByIdAndCompany_IdAndDeletedAtIsNull(userId, companyId)
			.orElseThrow(() -> ApiException.notFound(ErrorCode.VALIDATION_ERROR, "User not found"));
	}

	private UserEntity loadUser(AuthenticatedUser authenticatedUser, UUID userId) {
		UserEntity user = loadUser(authenticatedUser.companyId(), userId);
		ensureCanAccessBranch(authenticatedUser, user.getBranch().getId());
		return user;
	}

	private UUID scopedBranchId(AuthenticatedUser authenticatedUser, UUID requestedBranchId) {
		if (canAccessAllBranches(authenticatedUser)) {
			return requestedBranchId;
		}
		if (requestedBranchId != null && !authenticatedUser.branchId().equals(requestedBranchId)) {
			throw ApiException.forbidden(ErrorCode.PERMISSION_DENIED, "User cannot access another branch");
		}
		return authenticatedUser.branchId();
	}

	private void ensureCanAccessBranch(AuthenticatedUser authenticatedUser, UUID branchId) {
		if (!canAccessAllBranches(authenticatedUser) && !authenticatedUser.branchId().equals(branchId)) {
			throw ApiException.forbidden(ErrorCode.PERMISSION_DENIED, "User cannot access another branch");
		}
	}

	private boolean canAccessAllBranches(AuthenticatedUser authenticatedUser) {
		return COMPANY_ADMIN.equals(authenticatedUser.roleCode());
	}

	private BranchEntity loadActiveBranch(UUID companyId, UUID branchId) {
		BranchEntity branch = branchRepository.findByIdAndCompany_IdAndDeletedAtIsNull(branchId, companyId)
			.orElseThrow(() -> ApiException.notFound(ErrorCode.VALIDATION_ERROR, "Branch not found"));

		if (!branch.isActive()) {
			throw ApiException.badRequest(ErrorCode.VALIDATION_ERROR, "Branch must be active for this operation");
		}

		return branch;
	}

	private RoleEntity loadAccessibleRole(UUID companyId, UUID roleId) {
		return roleRepository.findAccessibleById(roleId, companyId)
			.orElseThrow(() -> ApiException.notFound(ErrorCode.VALIDATION_ERROR, "Role not found"));
	}

	private void ensureUsernameAvailable(UUID companyId, String username, UUID excludedUserId) {
		boolean exists = userRepository.existsByCompany_IdAndUsernameIgnoreCaseAndDeletedAtIsNull(companyId, username);
		if (!exists) {
			return;
		}

		if (excludedUserId != null) {
			UserEntity currentUser = userRepository.findByIdAndCompany_IdAndDeletedAtIsNull(excludedUserId, companyId)
				.orElseThrow(() -> ApiException.notFound(ErrorCode.VALIDATION_ERROR, "User not found"));

			if (currentUser.getUsername().equalsIgnoreCase(username)) {
				return;
			}
		}

		throw ApiException.badRequest(ErrorCode.VALIDATION_ERROR, "Username already exists in the company");
	}

	private void ensureUserNotDeleted(UserEntity user) {
		if (user.getStatus() == UserStatus.DELETED || user.getDeletedAt() != null) {
			throw ApiException.badRequest(ErrorCode.VALIDATION_ERROR, "Deleted users cannot be modified");
		}
	}

	private void preventSelfMutation(AuthenticatedUser authenticatedUser, UUID targetUserId, String message) {
		if (authenticatedUser.userId().equals(targetUserId)) {
			throw ApiException.badRequest(ErrorCode.VALIDATION_ERROR, message);
		}
	}

	private String normalizeUsername(String username) {
		return username.trim().toLowerCase(Locale.ROOT);
	}

	private String normalizeQuery(String query) {
		if (query == null) {
			return "";
		}

		String trimmed = query.trim();
		return trimmed.isEmpty() ? "" : trimmed;
	}

	private String trimToNull(String value) {
		if (value == null) {
			return null;
		}

		String trimmed = value.trim();
		return trimmed.isEmpty() ? null : trimmed;
	}
}
