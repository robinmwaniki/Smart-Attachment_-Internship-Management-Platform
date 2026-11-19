package com.library.smart_internship.config;

import com.library.smart_internship.repository.StudentRepository;
import com.library.smart_internship.security.RealmRoleConverter;
import com.library.smart_internship.service.LoginAttemptService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.oauth2.core.DelegatingOAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.OAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2TokenValidatorResult;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtValidators;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.HttpStatusEntryPoint;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final StudentRepository studentRepository;
    private final LoginAttemptService loginAttemptService;
    private final RealmRoleConverter realmRoleConverter;

    @Value("${spring.security.oauth2.resourceserver.jwt.issuer-uri}")
    private String issuerUri;

    @Value("${spring.security.oauth2.resourceserver.jwt.jwk-set-uri}")
    private String jwkSetUri;

    @Value("${keycloak.api-audience:smart-attachment-api}")
    private String apiAudience;

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public UserDetailsService userDetailsService() {
        return email -> {
            if (loginAttemptService.isLocked(email)) {
                throw new org.springframework.security.authentication.LockedException(
                        "Account temporarily locked due to too many failed login attempts. Try again in 15 minutes.");
            }

            var student = studentRepository.findAll().stream()
                    .filter(s -> s.getEmail().equalsIgnoreCase(email))
                    .findFirst()
                    .orElseThrow(() -> new UsernameNotFoundException("User not found with email: " + email));

            return User.builder()
                    .username(student.getEmail())
                    .password(student.getPassword())
                    .roles(student.getRole())
                    .build();
        };
    }

    @Bean
    public DaoAuthenticationProvider authenticationProvider() {
        DaoAuthenticationProvider authProvider = new DaoAuthenticationProvider(userDetailsService());
        authProvider.setPasswordEncoder(passwordEncoder());
        return authProvider;
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration config) throws Exception {
        return config.getAuthenticationManager();
    }

    @Bean
    public JwtAuthenticationConverter jwtAuthenticationConverter() {
        JwtAuthenticationConverter converter = new JwtAuthenticationConverter();
        converter.setJwtGrantedAuthoritiesConverter(realmRoleConverter);
        return converter;
    }

    @Bean
    public JwtDecoder jwtDecoder() {
        NimbusJwtDecoder decoder = NimbusJwtDecoder.withJwkSetUri(jwkSetUri).build();
        OAuth2TokenValidator<Jwt> issuerValidator = JwtValidators.createDefaultWithIssuer(issuerUri);
        OAuth2TokenValidator<Jwt> audienceValidator = jwt -> jwt.getAudience().contains(apiAudience)
                ? OAuth2TokenValidatorResult.success()
                : OAuth2TokenValidatorResult.failure(
                        new OAuth2Error("invalid_token", "Required API audience is missing.", null));
        decoder.setJwtValidator(new DelegatingOAuth2TokenValidator<>(issuerValidator, audienceValidator));
        return decoder;
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http,
                                                    JwtAuthenticationConverter jwtAuthenticationConverter) throws Exception {
        AuthenticationEntryPoint apiEntryPoint = new HttpStatusEntryPoint(HttpStatus.UNAUTHORIZED);

        http
                .csrf(csrf -> csrf.disable())
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.IF_REQUIRED))
                .exceptionHandling(exceptions -> exceptions
                        .defaultAuthenticationEntryPointFor(apiEntryPoint, request -> request.getRequestURI().startsWith("/api/")))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/", "/register", "/login", "/forgot-password", "/reset-password",
                                "/css/**", "/js/**").permitAll()
                        .requestMatchers("/api/account/link").authenticated()
                        .requestMatchers(HttpMethod.GET, "/api/applications/mine").hasRole("STUDENT")
                        .requestMatchers(HttpMethod.POST, "/api/applications").hasRole("STUDENT")
                        .requestMatchers(HttpMethod.GET, "/api/applications/internship/**").hasRole("COMPANY")
                        .requestMatchers(HttpMethod.PUT, "/api/applications/*/status").hasRole("COMPANY")
                        .requestMatchers(HttpMethod.GET, "/api/applications/download/**")
                            .hasAnyRole("STUDENT", "COMPANY")
                        // Other API routes stay closed until their ownership checks are migrated.
                        .requestMatchers("/api/**").denyAll()
                        .requestMatchers("/recruiter/**").hasRole("RECRUITER")
                        .requestMatchers("/dashboard", "/student/**").hasRole("STUDENT")
                        .anyRequest().authenticated()
                )
                .formLogin(form -> form
                        .loginPage("/login")
                        .successHandler((request, response, authentication) -> {
                            loginAttemptService.loginSucceeded(authentication.getName());

                            boolean isRecruiter = authentication.getAuthorities().stream()
                                    .anyMatch(a -> a.getAuthority().equals("ROLE_RECRUITER"));

                            response.sendRedirect(isRecruiter ? "/recruiter/dashboard" : "/student/dashboard");
                        })
                        .failureHandler((request, response, exception) -> {
                            String username = request.getParameter("username");
                            if (!(exception instanceof org.springframework.security.authentication.LockedException)) {
                                loginAttemptService.loginFailed(username);
                            }

                            response.sendRedirect(loginAttemptService.isLocked(username)
                                    ? "/login?locked" : "/login?error");
                        })
                        .permitAll()
                )
                .logout(logout -> logout.logoutSuccessUrl("/login?logout").permitAll())
                .oauth2ResourceServer(oauth2 -> oauth2
                        .jwt(jwt -> jwt.jwtAuthenticationConverter(jwtAuthenticationConverter)));

        return http.build();
    }
}
