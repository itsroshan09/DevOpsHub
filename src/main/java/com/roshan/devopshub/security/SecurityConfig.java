
package com.roshan.devopshub.security;

import com.roshan.devopshub.user.UserRepository;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import jakarta.servlet.DispatcherType;

@Configuration
public class SecurityConfig {

    @Bean
    public JwtAuthenticationFilter jwtAuthenticationFilter(
            JwtService jwtService,
            UserRepository userRepository
    ) {
        return new JwtAuthenticationFilter(jwtService, userRepository);
    }

    @Bean
    public SecurityFilterChain securityFilterChain(
            HttpSecurity http,
            JwtAuthenticationFilter jwtAuthenticationFilter)
            throws Exception {

        http
                .csrf(AbstractHttpConfigurer::disable)
                .formLogin(AbstractHttpConfigurer::disable)
                .httpBasic(AbstractHttpConfigurer::disable)
                .sessionManagement(session -> session
                        .sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .exceptionHandling(exceptions -> exceptions
                        .authenticationEntryPoint((request, response, exception) ->
                                response.sendError(
                                        HttpServletResponse.SC_UNAUTHORIZED)))

                .authorizeHttpRequests(authorize -> authorize
                                .dispatcherTypeMatchers(DispatcherType.ERROR).permitAll()

                                .requestMatchers(
                                        HttpMethod.POST,
                                        "/api/auth/register",
                                        "/api/auth/login"
                                ).permitAll()

                        // Incident creation: ADMIN and RESPONDER
                        .requestMatchers(
                                HttpMethod.POST,
                                "/api/incidents"
                        ).hasAnyRole("ADMIN", "RESPONDER")

                        // Incident assignment: ADMIN and RESPONDER
                        .requestMatchers(
                                HttpMethod.PUT,
                                "/api/incidents/*/assignee"
                        ).hasAnyRole("ADMIN", "RESPONDER")

                        // Status transitions: ADMIN and RESPONDER
                        .requestMatchers(
                                HttpMethod.POST,
                                "/api/incidents/*/transitions"
                        ).hasAnyRole("ADMIN", "RESPONDER")

                        // Add investigation updates: ADMIN and RESPONDER
                        .requestMatchers(
                                HttpMethod.POST,
                                "/api/incidents/*/updates"
                        ).hasAnyRole("ADMIN", "RESPONDER")
                                // Create services: ADMIN only
                                .requestMatchers(
                                        HttpMethod.POST,
                                        "/api/services"
                                ).hasRole("ADMIN")

// Read services: all authenticated roles
                                .requestMatchers(
                                        HttpMethod.GET,
                                        "/api/services",
                                        "/api/services/**"
                                ).hasAnyRole("ADMIN", "RESPONDER", "VIEWER")

                        // Read incidents, updates, and history: all roles
                        .requestMatchers(
                                HttpMethod.GET,
                                "/api/incidents",
                                "/api/incidents/**"
                        ).hasAnyRole("ADMIN", "RESPONDER", "VIEWER")

                        // Everything else still requires authentication
                        .anyRequest().authenticated()
                )

                .addFilterBefore(
                        jwtAuthenticationFilter,
                        UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }
}
