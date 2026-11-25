package com.heima.modules.resource.config;

import com.heima.security.config.SecurityProperties;
import com.heima.security.filter.JwtAuthenticationFilter;
import com.heima.security.handler.SecurityExceptionHandler;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.List;

@Configuration
@RequiredArgsConstructor
public class ResourceSecurityConfig {
    private final JwtAuthenticationFilter filter;
    private final SecurityExceptionHandler handler;
    private final SecurityProperties properties;

    @Bean
    @Order(1)
    SecurityFilterChain resourceSecurityFilterChain(HttpSecurity http) throws Exception {
        return http.securityMatcher("/api/resources/**", "/api/resource-categories", "/api/resource-disk-types")
                .csrf(csrf -> csrf.disable()).cors(cors -> cors.configurationSource(corsSource()))
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .exceptionHandling(errors -> errors.authenticationEntryPoint(handler).accessDeniedHandler(handler))
                .authorizeHttpRequests(auth -> auth.requestMatchers(HttpMethod.GET,
                                "/api/resources", "/api/resources/*", "/api/resources/*/links", "/api/resources/*/attachments/*",
                                "/api/resource-categories", "/api/resource-disk-types").permitAll()
                        .anyRequest().authenticated())
                .addFilterBefore(filter, UsernamePasswordAuthenticationFilter.class).build();
    }

    private CorsConfigurationSource corsSource() {
        CorsConfiguration config = new CorsConfiguration();
        config.setAllowedOrigins(properties.getAllowedOrigins());
        config.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS"));
        config.setAllowedHeaders(List.of("*"));
        config.setAllowCredentials(true);
        config.setMaxAge(3600L);
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        return source;
    }
}
