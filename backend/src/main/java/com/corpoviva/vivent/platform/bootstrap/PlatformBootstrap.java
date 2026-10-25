package com.corpoviva.vivent.platform.bootstrap;

import com.corpoviva.vivent.platform.config.PlatformProperties;
import com.corpoviva.vivent.platform.entity.PlatformUserEntity;
import com.corpoviva.vivent.platform.entity.PlatformUserStatus;
import com.corpoviva.vivent.platform.repository.PlatformUserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Component
public class PlatformBootstrap implements ApplicationRunner {

	private static final Logger log = LoggerFactory.getLogger(PlatformBootstrap.class);

	private final PlatformProperties platformProperties;
	private final PlatformUserRepository platformUserRepository;
	private final PasswordEncoder passwordEncoder;

	public PlatformBootstrap(
		PlatformProperties platformProperties,
		PlatformUserRepository platformUserRepository,
		PasswordEncoder passwordEncoder
	) {
		this.platformProperties = platformProperties;
		this.platformUserRepository = platformUserRepository;
		this.passwordEncoder = passwordEncoder;
	}

	@Override
	@Transactional
	public void run(ApplicationArguments args) {
		if (!platformProperties.bootstrap().enabled()) {
			return;
		}

		String username = platformProperties.bootstrap().username();
		String password = platformProperties.bootstrap().password();

		if (!StringUtils.hasText(username) || !StringUtils.hasText(password)) {
			log.warn(
				"Platform bootstrap is enabled but username or password is missing. "
					+ "Set vivent.platform.bootstrap.username and vivent.platform.bootstrap.password."
			);
			return;
		}

		if (platformUserRepository.existsByUsername(username.trim())) {
			log.info("Platform bootstrap user '{}' already exists; skipping creation.", username.trim());
			return;
		}

		PlatformUserEntity user = new PlatformUserEntity();
		user.setUsername(username.trim());
		user.setPasswordHash(passwordEncoder.encode(password));
		user.setDisplayName("Platform Operator");
		user.setStatus(PlatformUserStatus.ACTIVE);
		platformUserRepository.save(user);

		log.warn(
			"Platform bootstrap user '{}' was created. Disable vivent.platform.bootstrap.enabled after first startup.",
			user.getUsername()
		);
	}
}
