package com.minimarket.security.config;

import com.minimarket.security.service.LoginAttemptService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.LoginUrlAuthenticationEntryPoint;
import org.springframework.security.web.csrf.CookieCsrfTokenRepository;
import org.springframework.security.web.csrf.CsrfTokenRequestAttributeHandler;
import org.springframework.security.web.header.writers.ReferrerPolicyHeaderWriter;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfig {

    private static final Logger log = LoggerFactory.getLogger(SecurityConfig.class);

    private final LoginAttemptService loginAttemptService;

    public SecurityConfig(LoginAttemptService loginAttemptService) {
        this.loginAttemptService = loginAttemptService;
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        CookieCsrfTokenRepository csrfTokenRepository = new CookieCsrfTokenRepository();
        csrfTokenRepository.setCookieName("XSRF-TOKEN");
        csrfTokenRepository.setHeaderName("X-XSRF-TOKEN");
        csrfTokenRepository.setCookieHttpOnly(true);
        csrfTokenRepository.setCookieCustomizer(cookie -> cookie.sameSite("Lax"));

        CsrfTokenRequestAttributeHandler csrfRequestHandler = new CsrfTokenRequestAttributeHandler();
        csrfRequestHandler.setCsrfRequestAttributeName("_csrf");

        http
                // CSRF activo en /login y /logout (formulario HTML). /api/** exento para Postman REST.
                .csrf(csrf -> csrf
                        .csrfTokenRepository(csrfTokenRepository)
                        .csrfTokenRequestHandler(csrfRequestHandler)
                        .ignoringRequestMatchers("/api/**"))
                .headers(headers -> headers
                        .frameOptions(frame -> frame.sameOrigin())
                        .contentSecurityPolicy(csp -> csp
                                .policyDirectives("default-src 'self'; form-action 'self'; frame-ancestors 'self'; base-uri 'self'"))
                        .referrerPolicy(referrer -> referrer
                                .policy(ReferrerPolicyHeaderWriter.ReferrerPolicy.STRICT_ORIGIN_WHEN_CROSS_ORIGIN))
                        .httpStrictTransportSecurity(hsts -> hsts.disable()))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/public/**").permitAll()
                        .requestMatchers("/login", "/error").permitAll()
                        .requestMatchers("/h2-console/**").hasRole("GERENTE")
                        .requestMatchers(HttpMethod.GET, "/api/productos/**").hasAnyRole("CLIENTE", "EMPLEADO", "GERENTE")
                        .requestMatchers(HttpMethod.GET, "/api/categorias/**").hasAnyRole("CLIENTE", "EMPLEADO", "GERENTE")
                        .requestMatchers("/api/productos/**").hasAnyRole("EMPLEADO", "GERENTE")
                        .requestMatchers("/api/categorias/**").hasAnyRole("EMPLEADO", "GERENTE")
                        .requestMatchers("/api/carrito/**").hasAnyRole("CLIENTE", "EMPLEADO", "GERENTE")
                        .requestMatchers("/api/ventas/**").hasAnyRole("CLIENTE", "EMPLEADO", "GERENTE")
                        .requestMatchers("/api/inventario/**").hasAnyRole("EMPLEADO", "GERENTE")
                        .requestMatchers("/api/detalle-ventas/**").hasAnyRole("EMPLEADO", "GERENTE")
                        .requestMatchers("/api/usuarios/**").hasRole("GERENTE")
                        .anyRequest().authenticated()
                )
                .exceptionHandling(ex -> ex
                        .authenticationEntryPoint((request, response, authException) -> {
                            if (isApiRequest(request)) {
                                response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "No autenticado");
                            } else {
                                new LoginUrlAuthenticationEntryPoint("/login").commence(request, response, authException);
                            }
                        })
                        .accessDeniedHandler((request, response, accessDeniedException) -> {
                            String usuario = request.getUserPrincipal() != null
                                    ? request.getUserPrincipal().getName()
                                    : "anonimo";
                            log.warn("Acceso denegado para '{}': {} {}",
                                    usuario, request.getMethod(), request.getRequestURI());
                            response.sendError(HttpServletResponse.SC_FORBIDDEN, "Acceso denegado");
                        })
                )
                .formLogin(form -> form
                        .loginPage("/login")
                        .loginProcessingUrl("/login")
                        .defaultSuccessUrl("/public/hola", true)
                        .failureHandler((request, response, exception) -> {
                            String username = request.getParameter("username");
                            loginAttemptService.loginFailed(username);
                            log.warn("Autenticacion fallida para '{}': {}", username, exception.getMessage());
                            if (isApiRequest(request)) {
                                response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Credenciales invalidas");
                            } else {
                                response.sendRedirect("/login?error");
                            }
                        })
                        .successHandler((request, response, authentication) -> {
                            loginAttemptService.loginSucceeded(authentication.getName());
                            log.info("Login exitoso: {}", authentication.getName());
                            response.sendRedirect("/public/hola");
                        })
                )
                .logout(logout -> logout
                        .logoutUrl("/logout")
                        .logoutSuccessUrl("/public/hola")
                        .permitAll()
                );
        return http.build();
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration authConfig) throws Exception {
        return authConfig.getAuthenticationManager();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    /** API (/api/**) responde 401; navegador usa formulario HTML en /login. */
    private static boolean isApiRequest(HttpServletRequest request) {
        return request.getRequestURI().startsWith("/api/");
    }
}
