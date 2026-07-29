package com.lastkey.backend.security;

import com.lastkey.backend.security.handler.CustomAccessDeniedHandler;
import com.lastkey.backend.security.handler.CustomAuthenticationEntryPoint;
import com.lastkey.backend.security.jwt.JwtAuthenticationFilter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfig {

    private final JwtAuthenticationFilter
            jwtAuthenticationFilter;

    private final CustomAuthenticationEntryPoint
            customAuthenticationEntryPoint;

    private final CustomAccessDeniedHandler
            customAccessDeniedHandler;

    public SecurityConfig(
            JwtAuthenticationFilter jwtAuthenticationFilter,
            CustomAuthenticationEntryPoint
                    customAuthenticationEntryPoint,
            CustomAccessDeniedHandler
                    customAccessDeniedHandler
    ) {

        this.jwtAuthenticationFilter =
                jwtAuthenticationFilter;

        this.customAuthenticationEntryPoint =
                customAuthenticationEntryPoint;

        this.customAccessDeniedHandler =
                customAccessDeniedHandler;
    }

    @Bean
    public SecurityFilterChain securityFilterChain(
            HttpSecurity http
    ) throws Exception {

        http
                .csrf(csrf -> csrf.disable())

                .sessionManagement(session ->
                        session.sessionCreationPolicy(
                                SessionCreationPolicy.STATELESS
                        )
                )

                .exceptionHandling(exception ->
                        exception
                                .authenticationEntryPoint(
                                        customAuthenticationEntryPoint
                                )
                                .accessDeniedHandler(
                                        customAccessDeniedHandler
                                )
                )

                .authorizeHttpRequests(auth -> auth

                        .requestMatchers(
                                "/api/v1/auth/**",

                                "/swagger-ui/**",
                                "/swagger-ui.html",
                                "/v3/api-docs/**",
                                "/swagger-resources/**",
                                "/webjars/**",

                                "/actuator/health",

                                "/uploads/profile-images/**"
                        )
                        .permitAll()

                        .requestMatchers(
                                "/api/v1/notifications/**",
                                "/api/v1/nominee-access/**"
                        )
                        .authenticated()

                        .anyRequest()
                        .authenticated()
                )

                .addFilterBefore(
                        jwtAuthenticationFilter,
                        UsernamePasswordAuthenticationFilter.class
                );

        return http.build();
    }

    @Bean
    public AuthenticationManager
    authenticationManager(
            AuthenticationConfiguration configuration
    ) throws Exception {

        return configuration
                .getAuthenticationManager();
    }
}