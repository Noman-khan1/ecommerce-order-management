package com.noman.ecommerce_order_management.security;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@EnableMethodSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final RestAuthenticationEntryPoint
            restAuthenticationEntryPoint;

    private final RestAccessDeniedHandler
            restAccessDeniedHandler;

    @Bean
    public PasswordEncoder passwordEncoder() {

        return new BCryptPasswordEncoder();
    }

    @Bean
    public SecurityFilterChain securityFilterChain(
            HttpSecurity http
    ) throws Exception {

        http
                .csrf(
                        csrf ->
                                csrf.disable()
                )

                .sessionManagement(
                        session ->
                                session.sessionCreationPolicy(
                                        SessionCreationPolicy.STATELESS
                                )
                )

                .authorizeHttpRequests(
                        auth ->
                                auth

                                        /*
                                         * Spring's internal error endpoint.
                                         */
                                        .requestMatchers(
                                                "/error"
                                        )
                                        .permitAll()

                                        /*
                                         * Administration:
                                         *
                                         * catalog
                                         * warehouses
                                         * inventory
                                         * discounts
                                         */
                                        .requestMatchers(
                                                "/api/admin/**"
                                        )
                                        .hasRole(
                                                "ADMIN"
                                        )

                                        /*
                                         * Customer:
                                         *
                                         * catalog browsing
                                         * cart
                                         * pricing
                                         * checkout
                                         * order tracking
                                         * returns
                                         */
                                        .requestMatchers(
                                                "/api/customer/**"
                                        )
                                        .hasRole(
                                                "CUSTOMER"
                                        )

                                        /*
                                         * Warehouse staff:
                                         *
                                         * fulfillment lifecycle.
                                         */
                                        .requestMatchers(
                                                "/api/warehouse/**"
                                        )
                                        .hasRole(
                                                "WAREHOUSE_STAFF"
                                        )

                                        .anyRequest()
                                        .authenticated()
                )

                /*
                 * Security exceptions occur in the
                 * filter chain before @RestControllerAdvice.
                 *
                 * Therefore custom handlers are required
                 * for consistent JSON 401/403 responses.
                 */
                .exceptionHandling(
                        exception ->
                                exception
                                        .authenticationEntryPoint(
                                                restAuthenticationEntryPoint
                                        )
                                        .accessDeniedHandler(
                                                restAccessDeniedHandler
                                        )
                )

                /*
                 * HTTP Basic is deliberately sufficient
                 * for this take-home assignment.
                 *
                 * No JWT complexity is required.
                 */
                .httpBasic(
                        basic ->
                                basic.authenticationEntryPoint(
                                        restAuthenticationEntryPoint
                                )
                );

        return http.build();
    }
}