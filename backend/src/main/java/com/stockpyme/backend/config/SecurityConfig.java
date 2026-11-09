package com.stockpyme.backend.config;

import com.stockpyme.backend.seguridad.JwtFilter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.MediaType;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

/**
 * Seguridad de la API:
 * - Sin sesiones en el servidor: cada petición lleva su token JWT (stateless).
 * - Solo el login, la comprobación de estado y Swagger son públicos.
 * - Los permisos por rol se marcan en cada controlador con @PreAuthorize.
 */
@Configuration
@EnableMethodSecurity
public class SecurityConfig {

    private static final String ERROR_401 = "{\"estado\":401,\"mensaje\":\"Sesión no válida o caducada\"}";
    private static final String ERROR_403 = "{\"estado\":403,\"mensaje\":\"No tienes permiso para esta operación\"}";

    @Bean
    public SecurityFilterChain filtros(HttpSecurity http, JwtFilter jwtFilter) throws Exception {
        return http
                .csrf(AbstractHttpConfigurer::disable)
                .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(a -> a
                        .requestMatchers("/api/auth/login", "/api/salud").permitAll()
                        .requestMatchers("/swagger-ui.html", "/swagger-ui/**", "/v3/api-docs/**").permitAll()
                        .requestMatchers("/error").permitAll()
                        .anyRequest().authenticated())
                .exceptionHandling(e -> e
                        .authenticationEntryPoint((req, res, ex) -> escribir(res, 401, ERROR_401))
                        .accessDeniedHandler((req, res, ex) -> escribir(res, 403, ERROR_403)))
                .addFilterBefore(jwtFilter, UsernamePasswordAuthenticationFilter.class)
                .build();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    private static void escribir(jakarta.servlet.http.HttpServletResponse res, int estado, String json)
            throws java.io.IOException {
        res.setStatus(estado);
        res.setContentType(MediaType.APPLICATION_JSON_VALUE);
        res.setCharacterEncoding("UTF-8");
        res.getWriter().write(json);
    }
}
