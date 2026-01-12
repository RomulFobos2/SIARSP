package com.mai.siarsp.security;


import com.mai.siarsp.service.visitor.VisitorService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.access.AccessDeniedHandler;

@Configuration
@EnableWebSecurity
@Order(2)
public class SecurityConfigVisitor {

    private final VisitorService visitorService;
    private final BCryptPasswordEncoder bCryptPasswordEncoder;

    public SecurityConfigVisitor(VisitorService visitorService, BCryptPasswordEncoder bCryptPasswordEncoder) {
        this.visitorService = visitorService;
        this.bCryptPasswordEncoder = bCryptPasswordEncoder;
    }

    @Bean
    public SecurityFilterChain visitorSecurityFilterChain(HttpSecurity http, AccessDeniedHandler accessDeniedHandler) throws Exception {
        http
                .securityMatcher("/visitor/**", "/static/**", "/images/**", "/css/**", "/js/**")
                .csrf(AbstractHttpConfigurer::disable)
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(
                                "/visitor/registration",
                                "/visitor/check-username",
                                "/visitor/verify-code/**",
                                "/visitor/reset-password"
                        ).permitAll()
                        .requestMatchers("/visitor/**").hasRole("VISITOR")
                        .anyRequest().authenticated()
                )
                .formLogin(form -> form
                        .loginPage("/visitor/login")
                        .loginProcessingUrl("/visitor/login")
                        .failureUrl("/visitor/login?error=loginError")
                        .defaultSuccessUrl("/", true)
                        .permitAll()
                )
                .logout(logout -> logout.permitAll().logoutSuccessUrl("/"))
                .exceptionHandling(eh -> eh.accessDeniedHandler(accessDeniedHandler))
                .authenticationProvider(visitorAuthenticationProvider());
        return http.build();
    }

    @Bean
    public AuthenticationProvider visitorAuthenticationProvider() {
        DaoAuthenticationProvider provider = new DaoAuthenticationProvider(visitorService);
        provider.setPasswordEncoder(bCryptPasswordEncoder);
        return provider;
    }


}
