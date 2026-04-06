package com.andreyferraz.gestao.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;

import com.andreyferraz.gestao.module.usuario.Usuario;
import com.andreyferraz.gestao.module.usuario.UsuarioRepository;

@Configuration
public class SecurityConfig {

	@Bean
	PasswordEncoder passwordEncoder() {
		return new BCryptPasswordEncoder();
	}

	@Bean
	UserDetailsService userDetailsService(UsuarioRepository usuarioRepository) {
		return username -> usuarioRepository.findByUsername(username)
				.map(this::toUserDetails)
				.orElseThrow(() -> new UsernameNotFoundException("Usuario nao encontrado."));
	}

	@Bean
	SecurityFilterChain securityFilterChain(HttpSecurity http) {
		http
				.authorizeHttpRequests(auth -> auth
						.requestMatchers("/login", "/error").permitAll()
						.anyRequest().authenticated())
				.formLogin(form -> form
						.loginPage("/login")
						.defaultSuccessUrl("/dashboard", true)
						.permitAll())
				.logout(logout -> logout
						.logoutSuccessUrl("/login?logout")
						.permitAll());

		return http.build();
	}

	private UserDetails toUserDetails(Usuario usuario) {
		String role = usuario.getRole() == null || usuario.getRole().isBlank() ? "USER" : usuario.getRole();
		return User.withUsername(usuario.getUsername())
				.password(usuario.getSenha())
				.roles(role)
				.disabled(Boolean.FALSE.equals(usuario.getAtivo()))
				.build();
	}

}
