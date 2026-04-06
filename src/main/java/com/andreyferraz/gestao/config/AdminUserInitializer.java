package com.andreyferraz.gestao.config;

import java.util.UUID;

import org.springframework.boot.CommandLineRunner;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
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
			if (adminPassword == null || adminPassword.isBlank()) {
				throw new IllegalStateException("Defina app.bootstrap.admin.password para criar o usuario admin.");
			}

			String hashedPassword = passwordEncoder.encode(adminPassword);
			String upsertAdminSql = "INSERT INTO usuarios (id, username, senha, ativo, role) "
					+ "VALUES (?, 'admin', ?, 1, 'ADMIN') "
					+ "ON CONFLICT(username) DO UPDATE SET "
					+ "senha = excluded.senha, ativo = 1, role = 'ADMIN'";
			jdbcTemplate.update(
					upsertAdminSql,
					UUID.randomUUID().toString(),
					hashedPassword);
		};
	}
}
