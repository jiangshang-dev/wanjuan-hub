package com.heima.security.config;

import com.heima.security.filter.JwtAuthenticationFilter;
import com.heima.security.handler.SecurityExceptionHandler;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import java.util.List;

@Configuration
@EnableMethodSecurity
@RequiredArgsConstructor
public class SecurityConfig {
    private final JwtAuthenticationFilter filter;
    private final SecurityExceptionHandler handler;
    private final SecurityProperties properties;

    @Bean PasswordEncoder passwordEncoder() { return new BCryptPasswordEncoder(); }

    @Bean SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        return http.csrf(csrf->csrf.disable()).cors(cors->cors.configurationSource(corsSource()))
                .sessionManagement(s->s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .exceptionHandling(e->e.authenticationEntryPoint(handler).accessDeniedHandler(handler))
                .authorizeHttpRequests(auth->auth
                        .requestMatchers("/actuator/health/**","/v3/api-docs/**","/swagger-ui/**","/swagger-ui.html").permitAll()
                        .requestMatchers(org.springframework.http.HttpMethod.GET,
                                "/api/blogs/**","/api/blog-categories","/api/forum/**","/api/comments/**",
                                "/api/resources/**","/api/resource-categories","/api/announcements","/api/stats",
                                "/api/operation/**","/api/search/**","/api/rankings/**","/api/reactions/status","/api/membership/plans").permitAll()
                        .requestMatchers("/api/auth/captcha","/api/auth/code","/api/auth/sms-code","/api/auth/register","/api/auth/login","/api/auth/login/email","/api/auth/login/phone","/api/auth/refresh","/api/auth/password/reset","/api/admin/auth/login","/api/admin/auth/refresh").permitAll()
                        .requestMatchers("/api/payments/callback/alipay","/api/payments/callback/wechat").permitAll()
                        .requestMatchers("/api/admin/**").hasAuthority("ROLE_ADMIN")
                        .requestMatchers("/api/user/**","/api/auth/me","/api/auth/logout").hasRole("USER")
                        .anyRequest().authenticated())
                .addFilterBefore(filter,UsernamePasswordAuthenticationFilter.class).build();
    }

    private CorsConfigurationSource corsSource() {
        CorsConfiguration config=new CorsConfiguration(); config.setAllowedOrigins(properties.getAllowedOrigins());
        config.setAllowedMethods(List.of("GET","POST","PUT","DELETE","OPTIONS")); config.setAllowedHeaders(List.of("*"));
        config.setAllowCredentials(true); config.setMaxAge(3600L);
        UrlBasedCorsConfigurationSource source=new UrlBasedCorsConfigurationSource(); source.registerCorsConfiguration("/**",config); return source;
    }
}
