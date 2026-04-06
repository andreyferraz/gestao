package com.andreyferraz.gestao.config;

import java.util.UUID;

import org.springframework.boot.CommandLineRunner;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.PasswordEncoder;

import com.andreyferraz.gestao.module.usuario.Usuario;
import com.andreyferraz.gestao.module.usuario.UsuarioRepository;

@Configuration
public class AdminUserInitializer {

	@Bean
	CommandLineRunner createDefaultAdmin(
			UsuarioRepository usuarioRepository,
			PasswordEncoder passwordEncoder,
			@Value("${app.bootstrap.admin.password:}") String adminPassword) {
		return args -> {
			if (usuarioRepository.findByUsername("admin").isEmpty()) {
				if (adminPassword == null || adminPassword.isBlank()) {
					throw new IllegalStateException("Defina app.bootstrap.admin.password para criar o usuario admin.");
				}
				usuarioRepository.save(new Usuario(
						UUID.randomUUID(),
						"admin",
						passwordEncoder.encode(adminPassword),
						Boolean.TRUE,
						"ADMIN"));
			}
		};
	}
}
