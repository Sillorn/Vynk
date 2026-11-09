package com.vynk.shared.security;

import jakarta.servlet.http.HttpServletResponse;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.ProviderManager;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.session.SessionRegistry;
import org.springframework.security.core.session.SessionRegistryImpl;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.session.HttpSessionEventPublisher;
import org.springframework.security.web.authentication.logout.HttpStatusReturningLogoutSuccessHandler;
import org.springframework.security.web.csrf.CookieCsrfTokenRepository;
import org.springframework.security.web.csrf.CsrfTokenRequestAttributeHandler;
import org.springframework.security.web.header.writers.ReferrerPolicyHeaderWriter;
import org.springframework.security.web.header.writers.XXssProtectionHeaderWriter;

@Configuration
@EnableWebSecurity
@org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity
public class SecurityConfig {

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http, SessionRegistry sessionRegistry) throws Exception {
        CookieCsrfTokenRepository csrfTokenRepository = CookieCsrfTokenRepository.withHttpOnlyFalse();
        // Angular expects XSRF-TOKEN cookie; default name is XSRF-TOKEN and header X-XSRF-TOKEN
        csrfTokenRepository.setCookiePath("/");
        csrfTokenRepository.setCookieCustomizer(cookie -> {
            cookie.sameSite("Lax");
            cookie.secure(Boolean.parseBoolean(System.getenv().getOrDefault("VYNK_COOKIE_SECURE", "false")));
        });

        CsrfTokenRequestAttributeHandler requestHandler = new CsrfTokenRequestAttributeHandler();
        requestHandler.setCsrfRequestAttributeName("_csrf");

        http
                .csrf(csrf -> csrf
                        .csrfTokenRepository(csrfTokenRepository)
                        .csrfTokenRequestHandler(requestHandler)
                        .ignoringRequestMatchers(request -> "/api/v1/ping".equals(request.getRequestURI()) && "POST".equals(request.getMethod())))
                .sessionManagement(session -> session
                        .sessionCreationPolicy(SessionCreationPolicy.IF_REQUIRED)
                        .sessionFixation().migrateSession()
                        // Sessions are registered by AuthController at login. The registry lets a
                        // password change or account deletion expire the user's other sessions;
                        // an expired session is answered with 401 on its next request.
                        .maximumSessions(1)
                        .sessionRegistry(sessionRegistry)
                        .expiredSessionStrategy(event ->
                                event.getResponse().setStatus(HttpServletResponse.SC_UNAUTHORIZED)))
                .securityContext(securityContext -> securityContext
                        .requireExplicitSave(false))
                .headers(headers -> headers
                        .contentTypeOptions(contentTypeOptions -> {
                        })
                        .xssProtection(xss -> xss.headerValue(XXssProtectionHeaderWriter.HeaderValue.DISABLED))
                        .frameOptions(frame -> frame.deny())
                        .httpStrictTransportSecurity(hsts -> hsts
                                .includeSubDomains(true)
                                .maxAgeInSeconds(31536000)
                                .requestMatcher(request -> Boolean.parseBoolean(
                                        System.getenv().getOrDefault("VYNK_COOKIE_SECURE", "false"))))
                        .referrerPolicy(referrer -> referrer.policy(ReferrerPolicyHeaderWriter.ReferrerPolicy.NO_REFERRER))
                        .permissionsPolicyHeader(permissions -> permissions.policy(
                                "camera=(), microphone=(), geolocation=()")))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(HttpMethod.GET, "/api/v1/ping", "/api/v1/health").permitAll()
                        .requestMatchers("/actuator/health").permitAll()
                        .requestMatchers("/v3/api-docs/**").permitAll()
                        .requestMatchers(HttpMethod.POST, "/api/v1/auth/login", "/api/v1/auth/register").permitAll()
                        .requestMatchers(HttpMethod.POST, "/api/v1/auth/logout").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/v1/auth/me").authenticated()
                        .requestMatchers(HttpMethod.POST, "/api/v1/auth/password").authenticated()
                        .requestMatchers(HttpMethod.DELETE, "/api/v1/auth/account").authenticated()
                        .requestMatchers(HttpMethod.GET, "/api/v1/profiles/me").authenticated()
                        .requestMatchers("/api/v1/profiles/me/**").authenticated()
                        .requestMatchers(HttpMethod.GET, "/api/v1/profiles/**").permitAll()
                        .anyRequest().permitAll())
                .exceptionHandling(ex -> ex
                        .authenticationEntryPoint((request, response, authException) -> {
                            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                        })
                        .accessDeniedHandler(new ApiAccessDeniedHandler()))
                .logout(logout -> logout
                        .logoutUrl("/api/v1/auth/logout")
                        .deleteCookies("JSESSIONID", "XSRF-TOKEN")
                        .invalidateHttpSession(true)
                        .clearAuthentication(true)
                        .logoutSuccessHandler(new HttpStatusReturningLogoutSuccessHandler()));

        return http.build();
    }

    @Bean
    public SessionRegistry sessionRegistry() {
        return new SessionRegistryImpl();
    }

    /** Forwards servlet session lifecycle events so the registry and SSE streams are cleaned up. */
    @Bean
    public HttpSessionEventPublisher httpSessionEventPublisher() {
        return new HttpSessionEventPublisher();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public AuthenticationManager authenticationManager(UserDetailsService userDetailsService,
                                                       PasswordEncoder passwordEncoder) {
        DaoAuthenticationProvider provider = new DaoAuthenticationProvider(userDetailsService);
        provider.setPasswordEncoder(passwordEncoder);
        return new ProviderManager(provider);
    }
}
