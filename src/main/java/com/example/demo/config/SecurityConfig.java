package com.example.demo.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;

import com.example.demo.security.JwtAuthenticationFilter;

@Configuration
@EnableMethodSecurity
public class SecurityConfig {

        @Bean
        public SecurityFilterChain securityFilterChain(
                        HttpSecurity http,
                        JwtAuthenticationFilter jwtAuthenticationFilter) throws Exception {

                http
                                // ❌ Disable CSRF (REST API)
                                .csrf(AbstractHttpConfigurer::disable)

                                // ❌ Disable default auth mechanisms
                                .formLogin(AbstractHttpConfigurer::disable)
                                .httpBasic(AbstractHttpConfigurer::disable)

                                // ✅ Stateless JWT
                                .sessionManagement(session -> session
                                                .sessionCreationPolicy(SessionCreationPolicy.STATELESS))

                                // ✅ Authorization rules
                                .authorizeHttpRequests(auth -> auth

                                                // Allow preflight
                                                .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()

                                                // PUBLIC APIs (must stay public)
                                                .requestMatchers("/api/auth/**").permitAll()
                                                .requestMatchers("/test/hello").permitAll()

                                                // PROTECTED APIs (require JWT)
                                                .requestMatchers("/api/users/**").authenticated()

                                                // Static resources
                                                .requestMatchers(
                                                                "/css/**",
                                                                "/js/**",
                                                                "/images/**",
                                                                "/static/**",
                                                                "/*.html",
                                                                "/favicon.ico")
                                                .permitAll()

                                                // Frontend pages (Thymeleaf templates)
                                                .requestMatchers(
                                                                "/",
                                                                "/login",
                                                                "/register",
                                                                "/dashboard",
                                                                "/forgot-password",
                                                                "/reset-password")
                                                .permitAll()

                                                // Everything else requires JWT
                                                .anyRequest().authenticated())

                                // ✅ JWT filter runs BEFORE username/password auth
                                .addFilterBefore(
                                                jwtAuthenticationFilter,
                                                UsernamePasswordAuthenticationFilter.class);

                return http.build();
        }
}
