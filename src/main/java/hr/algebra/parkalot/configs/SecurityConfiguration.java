package hr.algebra.parkalot.configs;

import hr.algebra.parkalot.models.enums.Role;
import hr.algebra.parkalot.services.CustomUserDetailsService;
import lombok.AllArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.context.NullSecurityContextRepository;
import org.springframework.security.web.csrf.CookieCsrfTokenRepository;

@Configuration
@EnableAsync
@EnableWebSecurity
@EnableMethodSecurity
@AllArgsConstructor
public class SecurityConfiguration {
    private final CustomUserDetailsService userDetailsService;
    private final TokenFilter tokenFilter;
    private final PasswordEncoder passwordEncoder;
    private final AuthEntryPoint unauthorizedHandler;
    private final LoginHandler loginHandler;
    private final LogoutHandler logoutHandler;

    private static final String[] REST_PUBLIC_WHITELIST = { "/rest/auth/**", "/logout", "/rest/public/**", "/" };
    private static final String[] MVC_PUBLIC_WHITELIST = { "/logout", "/mvc/public/**", "/" };
    private static final String[] MVC_ADMIN_LIST = { "/mvc/admin/**" };
    private static final String[] REST_ADMIN_LIST = { "/rest/admin/**" };

    @Bean
    @Order(1)
    public SecurityFilterChain apiChain(HttpSecurity http) {
        http
            .headers(headers -> headers
                    .contentSecurityPolicy(csp -> csp
                            .policyDirectives(
                                    "default-src 'self'; " +
                                            "script-src 'self' https://maps.googleapis.com" +
                                            "  https://www.paypal.com" +
                                            "  https://*.paypal.com" +
                                            "  https://www.paypalobjects.com" +
                                            "  https://*.paypalobjects.com; " +
                                            "style-src 'self' 'unsafe-inline'; " +
                                            "img-src 'self' data: https:; " +
                                            "font-src 'self'; " +
                                            "connect-src 'self' https://www.paypal.com https://*.paypal.com https://maps.googleapis.com; " +
                                            "frame-src https://www.paypal.com https://*.paypal.com; " +
                                            "object-src 'none'; " +
                                            "base-uri 'self'; " +
                                            "form-action 'self';"
                            )
                    )
            )
            .securityMatcher("/rest/**")
            .csrf(AbstractHttpConfigurer::disable)
            .sessionManagement(sess -> sess.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .securityContext(sc ->
                    sc.securityContextRepository(new NullSecurityContextRepository())
            )
            .authorizeHttpRequests(auth ->
                    auth.requestMatchers(REST_PUBLIC_WHITELIST).permitAll()
                            .requestMatchers(REST_ADMIN_LIST).hasRole(Role.ADMIN.getRoleName())
                            .anyRequest().hasAnyRole(
                                    Role.ADMIN.getRoleName(),
                                    Role.OFFICER.getRoleName(),
                                    Role.USER.getRoleName()
                            )
            )
            .authenticationProvider(authenticationProvider(passwordEncoder))
            .exceptionHandling(ex -> ex.authenticationEntryPoint(unauthorizedHandler))
            .addFilterBefore(tokenFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    @Bean
    @Order(2)
    public SecurityFilterChain mvcChain(HttpSecurity http) {
        http
                .headers(headers -> headers
                        .contentSecurityPolicy(csp -> csp
                                .policyDirectives(
                                        "default-src 'self'; " +
                                                "script-src 'self' https://maps.googleapis.com" +
                                                "  https://www.paypal.com" +
                                                "  https://*.paypal.com" +
                                                "  https://www.paypalobjects.com" +
                                                "  https://*.paypalobjects.com; " +
                                                "style-src 'self' 'unsafe-inline'; " +
                                                "img-src 'self' data: https:; " +
                                                "font-src 'self'; " +
                                                "connect-src 'self' https://www.paypal.com https://*.paypal.com https://maps.googleapis.com; " +
                                                "frame-src https://www.paypal.com https://*.paypal.com; " +
                                                "object-src 'none'; " +
                                                "base-uri 'self'; " +
                                                "form-action 'self';"
                                )
                        )
                )
                .userDetailsService(userDetailsService)
                .csrf(csrf -> csrf
                        .csrfTokenRepository(new CookieCsrfTokenRepository()))
                .sessionManagement(sess -> sess.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(authorize ->
                        authorize
                                .requestMatchers(MVC_PUBLIC_WHITELIST).permitAll()
                                .requestMatchers(
                                        "/js/**",
                                        "/css/**",
                                        "/images/**",
                                        "/webjars/**",
                                        "/favicon.ico"
                                ).permitAll()
                                .requestMatchers(MVC_ADMIN_LIST).hasRole(Role.ADMIN.getRoleName())
                                .anyRequest().hasAnyRole(Role.ADMIN.getRoleName(), Role.OFFICER.getRoleName(), Role.USER.getRoleName())
                )
                .addFilterBefore(tokenFilter, UsernamePasswordAuthenticationFilter.class)
                .formLogin(httpSecurityFormLoginConfigurer -> httpSecurityFormLoginConfigurer
                        .loginPage("/mvc/public/auth/login")
                        .loginProcessingUrl("/login")
                        .usernameParameter("email")
                        .successHandler(loginHandler)
                        .permitAll())
                .logout(logout -> logout
                        .logoutUrl("/logout")
                        .logoutSuccessHandler(logoutHandler)
                        .permitAll()
                );
        return http.build();
    }

    @Bean
    public AuthenticationManager authenticationManager(HttpSecurity http, PasswordEncoder encoder) {

        AuthenticationManagerBuilder authBuilder =
                http.getSharedObject(AuthenticationManagerBuilder.class);

        authBuilder
                .userDetailsService(userDetailsService)
                .passwordEncoder(encoder);

        return authBuilder.build();
    }

    @Bean
    public DaoAuthenticationProvider authenticationProvider(PasswordEncoder encoder) {
        DaoAuthenticationProvider auth = new DaoAuthenticationProvider(userDetailsService);
        auth.setPasswordEncoder(encoder);
        return auth;
    }

    @Autowired
    public void configureGlobal(AuthenticationManagerBuilder auth, PasswordEncoder encoder) {
        auth.userDetailsService(userDetailsService)
                .passwordEncoder(encoder);
    }
}
