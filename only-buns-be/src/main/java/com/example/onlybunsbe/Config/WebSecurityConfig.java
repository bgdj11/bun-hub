package com.example.onlybunsbe.Config;

import com.example.onlybunsbe.infrastructure.monitoring.ActiveUserFilter;
import com.example.onlybunsbe.infrastructure.monitoring.ActiveUserTracker;
import com.example.onlybunsbe.security.auth.RestAuthenticationEntryPoint;
import com.example.onlybunsbe.security.auth.TokenAuthenticationFilter;
import com.example.onlybunsbe.service.CustomUserDetailsService;
import com.example.onlybunsbe.util.TokenUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.SecurityConfigurerAdapter;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityCustomizer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.authentication.www.BasicAuthenticationFilter;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity(prePostEnabled = true, securedEnabled = true, jsr250Enabled = true)
public class WebSecurityConfig {

    @Autowired
    private RestAuthenticationEntryPoint restAuthenticationEntryPoint;

    @Autowired
    private TokenUtils tokenUtils;

    @Autowired
    private ActiveUserTracker activeUserTracker;

    @Bean
    public UserDetailsService userDetailsService() {
        return new CustomUserDetailsService();
    }

    @Bean
    public BCryptPasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public DaoAuthenticationProvider authenticationProvider() {
        DaoAuthenticationProvider authProvider = new DaoAuthenticationProvider();
        authProvider.setUserDetailsService(userDetailsService());
        authProvider.setPasswordEncoder(passwordEncoder());
        return authProvider;
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration authConfig) throws Exception {
        return authConfig.getAuthenticationManager();
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http.sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .exceptionHandling(exception -> exception.authenticationEntryPoint(restAuthenticationEntryPoint))
                .authorizeHttpRequests(authorize -> authorize
                        .requestMatchers("/uploads/**").permitAll()
                        .requestMatchers("/favicon.ico").permitAll()
                        .requestMatchers("/error").permitAll() // Dozvoljava pristup error handler-u
                        .requestMatchers("/actuator/prometheus").permitAll() // Dozvoljava pristup bez autentikacije
                        .requestMatchers("/auth/**").permitAll()
                        .requestMatchers("/api/posts/ad-eligibility/**").hasRole("ADMIN")
                        .requestMatchers("/api/posts/**").permitAll()
                        .requestMatchers("/api/user/**").authenticated()
                        .requestMatchers("/api/likes/**").authenticated()// `ROLE_USER` će se automatski prepoznati
                        .requestMatchers("/api/comments/**").authenticated() // Više uloga, korisnik mora imati barem jednu
                        .requestMatchers("/h2-console/**").permitAll()
                        .requestMatchers("/api/foo").permitAll()
                        .requestMatchers("/api/trends").authenticated()
                        .requestMatchers("/swagger-ui/**").permitAll()
                        .requestMatchers("/api/analytics/**").hasAnyRole("ADMIN")
                        .requestMatchers("/api/groups/**").authenticated()
                        .requestMatchers("/api/chat/**").permitAll()
                        .requestMatchers("/api/follow/**").authenticated()
                        .anyRequest().authenticated()
                )
                .csrf(csrf -> csrf.disable())
                .cors(Customizer.withDefaults())
                .addFilterBefore(new TokenAuthenticationFilter(tokenUtils, userDetailsService()), UsernamePasswordAuthenticationFilter.class)
                .addFilterAfter(new ActiveUserFilter(activeUserTracker), TokenAuthenticationFilter.class);


        http.authenticationProvider(authenticationProvider());

        return http.build();
    }

    @Bean
    public WebSecurityCustomizer webSecurityCustomizer() {
        return (web) -> web.ignoring().requestMatchers(
                "/swagger-ui/**",
                "/api-docs/**",
                "/swagger-resources/**",
                "/webjars/**",
                "/uploads/**",
                "/actuator/prometheus"
        );
    }
}
