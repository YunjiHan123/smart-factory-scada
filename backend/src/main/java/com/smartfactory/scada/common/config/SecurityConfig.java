package com.smartfactory.scada.common.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.configurers.ExceptionHandlingConfigurer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

import com.smartfactory.scada.auth.filter.JwtAuthenticationFilter;
import com.smartfactory.scada.auth.security.JwtAuthenticationEntryPoint;

import lombok.RequiredArgsConstructor;

@Configuration
@RequiredArgsConstructor
public class SecurityConfig {

	private final JwtAuthenticationFilter jwtAuthenticationFilter;
	private final JwtAuthenticationEntryPoint jwtAuthenticationEntryPoint;

	@Bean
	public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
		return http
			.csrf(AbstractHttpConfigurer::disable)
			.formLogin(AbstractHttpConfigurer::disable)
			.httpBasic(AbstractHttpConfigurer::disable)
			.sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
			.exceptionHandling(this::configureExceptionHandling)
			.authorizeHttpRequests(auth -> auth
				.requestMatchers(
					"/api/auth/signup",
					"/api/auth/login",
					"/api/auth/refresh",
					"/swagger-ui/**",
					"/swagger-ui.html",
					"/v3/api-docs/**",
					"/v3/api-docs.yaml",
					"/actuator/health",
					"/actuator/health/**",
					"/actuator/info",
					"/webjars/**",
					"/ws/**",
					"/favicon.ico",
					"/error"
				).permitAll()
				.anyRequest().authenticated()
			)
			.addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class)
			.build();
	}

	private void configureExceptionHandling(ExceptionHandlingConfigurer<HttpSecurity> exceptionHandling) {
		exceptionHandling.authenticationEntryPoint(jwtAuthenticationEntryPoint);
	}
}
