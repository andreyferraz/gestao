package com.andreyferraz.gestao.config;

import java.util.UUID;

import org.springframework.boot.CommandLineRunner;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;

@Configuration
public class AdminUserInitializer {

	@Bean
	CommandLineRunner createDefaultAdmin(
			JdbcTemplate jdbcTemplate,
			PasswordEncoder passwordEncoder,
			@Value("${app.bootstrap.admin.password:}") String adminPassword) {
		return args -> {
			if (adminExists(jdbcTemplate)) {
				return;
			}

			if (adminPassword == null || adminPassword.isBlank()) {
				throw new IllegalStateException(
						"Defina APP_BOOTSTRAP_ADMIN_PASSWORD para criar o usuario admin.");
			}

			String hashedPassword = passwordEncoder.encode(adminPassword);
			try {
				jdbcTemplate.update(
						"INSERT INTO usuarios (id, username, senha, ativo, role) VALUES (?, 'admin', ?, 1, 'ADMIN')",
						UUID.randomUUID().toString(),
						hashedPassword);
			} catch (DataAccessException insertFailure) {
				try {
					if (adminExists(jdbcTemplate)) {
						return;
					}
				} catch (RuntimeException verificationFailure) {
					if (verificationFailure != insertFailure) {
						insertFailure.addSuppressed(verificationFailure);
					}
				}
				throw insertFailure;
			}
		};
	}

	private boolean adminExists(JdbcTemplate jdbcTemplate) {
		Integer existingAdminCount = jdbcTemplate.queryForObject(
				"SELECT COUNT(*) FROM usuarios WHERE username = 'admin'",
				Integer.class);
		return existingAdminCount != null && existingAdminCount > 0;
	}
}
