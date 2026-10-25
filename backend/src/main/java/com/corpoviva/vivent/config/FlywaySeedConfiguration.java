package com.corpoviva.vivent.config;

import javax.sql.DataSource;
import org.flywaydb.core.Flyway;
import org.flywaydb.core.api.MigrationVersion;
import org.springframework.boot.flyway.autoconfigure.FlywayMigrationStrategy;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class FlywaySeedConfiguration {

	@Bean
	public FlywayMigrationStrategy flywayMigrationStrategy(DataSource dataSource) {
		return flyway -> {
			flyway.migrate();

			Flyway.configure()
				.dataSource(dataSource)
				.locations("classpath:db/seed")
				.sqlMigrationPrefix("S")
				.table("flyway_seed_history")
				.baselineOnMigrate(true)
				.baselineVersion(MigrationVersion.fromVersion("0"))
				.validateMigrationNaming(true)
				.outOfOrder(true)
				.load()
				.migrate();
		};
	}
}
