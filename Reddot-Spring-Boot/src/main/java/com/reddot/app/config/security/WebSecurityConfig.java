package com.reddot.app.config.security;

import com.reddot.app.config.security.jwt.JwtTokenFilter;
import com.reddot.app.service.user.UserServiceManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Lazy;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
@EnableWebSecurity
public class WebSecurityConfig {
    final UserServiceManager userServiceManager;
    final JwtTokenFilter jwtTokenFilter;

    public WebSecurityConfig(@Lazy UserServiceManager userServiceManager, JwtTokenFilter jwtTokenFilter) {
        this.userServiceManager = userServiceManager;
        this.jwtTokenFilter = jwtTokenFilter;
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http // Disable CSRF for APIs
                .csrf(AbstractHttpConfigurer::disable)

                // Set permissions for different endpoints
                .securityMatcher("/questions/**", "/comments/**", "/answers/**", "/users/**", "/settings/**", "/private/**", "/admin/**", "/notifications/*")
                .authorizeHttpRequests(auth -> auth
                        // Public endpoints
                        .requestMatchers("/hello", "/auth/**", "/questions/search").permitAll()
                        .requestMatchers("/users", "/users/{id:[\\d]+}").permitAll()
                        .requestMatchers(HttpMethod.GET, "/users", "/users/{ids:[\\d,]+}/bookmarks").permitAll()
                        .requestMatchers(HttpMethod.GET, "/questions", "/questions/{ids:[\\d,]+}").permitAll()
                        .requestMatchers(HttpMethod.GET, "/comments", "/comments/{ids:[\\d,]+}").permitAll()
                        .requestMatchers(HttpMethod.GET, "/notifications", "/notifications/*").permitAll()
                        .requestMatchers("/settings/reset-password", "/settings/reset-password/confirm", "/settings/email/confirm", "/settings/email/resend-confirm").permitAll()

                        // Private endpoints accessible by role
                        .requestMatchers("/private").hasRole("USER")
                        .requestMatchers("/admin/**").hasRole("ADMIN")

                        // OpenAPI endpoints
                        .requestMatchers("/v3/**", "/swagger-ui/**", "/swagger-ui.html").permitAll()
                        .anyRequest().authenticated())

                // Set session management:
                // -STATELESS: No session will be created, each request will be authenticated individually.
                // Read more about Authentication Persistence and Session Management
                .sessionManagement(sess -> sess.sessionCreationPolicy(SessionCreationPolicy.STATELESS));

        // Add JWT filter
        http.addFilterBefore(jwtTokenFilter, UsernamePasswordAuthenticationFilter.class);
        return http.build();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration authenticationConfiguration) throws Exception {
        return authenticationConfiguration.getAuthenticationManager();
    }

    @Bean
    public DaoAuthenticationProvider authenticationProvider() {
        DaoAuthenticationProvider authProvider = new DaoAuthenticationProvider();
        authProvider.setUserDetailsService(this.userServiceManager);
        authProvider.setPasswordEncoder(passwordEncoder());
        return authProvider;
    }

    @Bean
    public WebMvcConfigurer corsConfigurer() {
        return new WebMvcConfigurer() {
            @Override
            public void addCorsMappings(CorsRegistry registry) {
                registry.addMapping("/**") // Apply to all endpoints
                        .allowedOrigins("http://localhost:3000")
                        .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS")
                        .allowedHeaders("*") // Allow all headers
                        .allowCredentials(true) // Allow cookies
                        .maxAge(3600); // Cache preflight request for 1 hour
            }
        };
    }
}