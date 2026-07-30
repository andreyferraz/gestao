package com.andreyferraz.gestao.config;

import java.util.List;

import jakarta.servlet.http.HttpServletRequest;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.AuthenticationTrustResolver;
import org.springframework.security.authentication.AuthenticationTrustResolverImpl;
import org.springframework.security.authentication.InsufficientAuthenticationException;
import org.springframework.security.config.ObjectPostProcessor;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.security.web.access.AccessDeniedHandlerImpl;
import org.springframework.security.web.authentication.www.BasicAuthenticationConverter;
import org.springframework.security.web.authentication.www.BasicAuthenticationEntryPoint;
import org.springframework.security.web.csrf.CsrfFilter;
import org.springframework.security.web.util.matcher.RequestMatcher;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

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
	SecurityFilterChain securityFilterChain(
			HttpSecurity http,
			BasicAuthenticationEntryPoint apiBasicAuthenticationEntryPoint,
			AuthenticationConfiguration authenticationConfiguration) {
		RequestMatcher apiRequest = request -> request.getRequestURI()
				.startsWith(request.getContextPath() + "/api/");
		AuthenticationManager authenticationManager =
				authenticationConfiguration.getAuthenticationManager();

		http
				.cors(Customizer.withDefaults())
				.csrf(csrf -> csrf.withObjectPostProcessor(
						new ObjectPostProcessor<CsrfFilter>() {
							@Override
							public <O extends CsrfFilter> O postProcess(O filter) {
								filter.setAccessDeniedHandler(apiCsrfAccessDeniedHandler(
										apiRequest,
										apiBasicAuthenticationEntryPoint,
										authenticationManager));
								return filter;
							}
						}))
				.authorizeHttpRequests(auth -> auth
						.requestMatchers(
								HttpMethod.GET,
								"/api/projetos",
								"/api/projetos/**")
						.permitAll()
						.requestMatchers(
								HttpMethod.HEAD,
								"/api/projetos",
								"/api/projetos/**")
						.permitAll()
						.requestMatchers(HttpMethod.POST, "/api/projetos")
						.hasRole("ADMIN")
						.requestMatchers(HttpMethod.PUT, "/api/projetos/**")
						.hasRole("ADMIN")
						.requestMatchers(HttpMethod.DELETE, "/api/projetos/**")
						.hasRole("ADMIN")
						.requestMatchers("/login", "/error", "/css/**", "/js/**", "/images/**", "/favicon.ico")
						.permitAll()
						.anyRequest().authenticated())
				.exceptionHandling(exceptions -> exceptions
						.defaultAuthenticationEntryPointFor(
								apiBasicAuthenticationEntryPoint,
								apiRequest))
				.httpBasic(basic -> basic
						.authenticationEntryPoint(apiBasicAuthenticationEntryPoint))
				.formLogin(form -> form
						.loginPage("/login")
						.defaultSuccessUrl("/dashboard", true)
						.permitAll())
				.logout(logout -> logout
						.logoutSuccessUrl("/login?logout")
						.permitAll());

		return http.build();
	}

	@Bean
	BasicAuthenticationEntryPoint apiBasicAuthenticationEntryPoint() {
		BasicAuthenticationEntryPoint entryPoint = new BasicAuthenticationEntryPoint();
		entryPoint.setRealmName("gestao");
		return entryPoint;
	}

	@Bean
	CorsConfigurationSource corsConfigurationSource(
			@Value("${app.cors.allowed-origins:https://www.andreyferraz.com.br}")
			List<String> allowedOrigins) {
		CorsConfiguration configuration = new CorsConfiguration();
		configuration.setAllowedOrigins(allowedOrigins);
		configuration.setAllowedMethods(List.of(
				HttpMethod.GET.name(),
				HttpMethod.HEAD.name(),
				HttpMethod.OPTIONS.name()));
		configuration.setAllowedHeaders(List.of(
				HttpHeaders.ACCEPT,
				HttpHeaders.CONTENT_TYPE));
		configuration.setAllowCredentials(false);

		UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
		source.registerCorsConfiguration("/api/projetos", configuration);
		source.registerCorsConfiguration("/api/projetos/**", configuration);
		return source;
	}

	private AccessDeniedHandler apiCsrfAccessDeniedHandler(
			RequestMatcher apiRequest,
			BasicAuthenticationEntryPoint apiBasicAuthenticationEntryPoint,
			AuthenticationManager authenticationManager) {
		AuthenticationTrustResolver trustResolver = new AuthenticationTrustResolverImpl();
		AccessDeniedHandler defaultAccessDeniedHandler = new AccessDeniedHandlerImpl();
		BasicAuthenticationConverter basicAuthenticationConverter =
				new BasicAuthenticationConverter();

		return (request, response, accessDeniedException) -> {
			Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
			boolean unauthenticated = authentication == null || trustResolver.isAnonymous(authentication);

			if (apiRequest.matches(request) && unauthenticated) {
				if (hasValidBasicCredentials(
						request,
						basicAuthenticationConverter,
						authenticationManager)) {
					defaultAccessDeniedHandler.handle(request, response, accessDeniedException);
					return;
				}

				apiBasicAuthenticationEntryPoint.commence(
						request,
						response,
						new InsufficientAuthenticationException(
								"Autenticacao necessaria para modificar recursos da API.",
								accessDeniedException));
				return;
			}

			defaultAccessDeniedHandler.handle(request, response, accessDeniedException);
		};
	}

	private boolean hasValidBasicCredentials(
			HttpServletRequest request,
			BasicAuthenticationConverter basicAuthenticationConverter,
			AuthenticationManager authenticationManager) {
		try {
			Authentication credentials = basicAuthenticationConverter.convert(request);
			if (credentials == null) {
				return false;
			}

			Authentication authenticated = authenticationManager.authenticate(credentials);
			return authenticated != null && authenticated.isAuthenticated();
		} catch (AuthenticationException exception) {
			return false;
		}
	}

	private UserDetails toUserDetails(Usuario usuario) {
		String role = usuario.getRole() == null || usuario.getRole().isBlank() ? "USER" : usuario.getRole();
		boolean disabled = usuario.getAtivo() == null || usuario.getAtivo() != 1;
		return User.withUsername(usuario.getUsername())
				.password(usuario.getSenha())
				.roles(role)
				.disabled(disabled)
				.build();
	}

}
