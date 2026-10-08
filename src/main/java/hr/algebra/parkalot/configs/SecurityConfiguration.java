package hr.algebra.parkalot.configs;


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
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
@EnableAsync
@EnableWebSecurity
@AllArgsConstructor
public class SecurityConfiguration {
    private final CustomUserDetailsService userDetailsService;
    private final TokenFilter tokenFilter;
    private final PasswordEncoder passwordEncoder;
    private final AuthEntryPoint unauthorizedHandler;

    private static final String[] AUTH_WHITELIST = { "/auth/**", "/logout", "/mvc/public/**", "/rest/public/**", "/rest/ai/**", "/" };

    @Bean
    @Order(1)
    public SecurityFilterChain apiChain(HttpSecurity http) {
        http
                .securityMatcher("/rest/**")
                .csrf(AbstractHttpConfigurer::disable)
                .sessionManagement(sess -> sess.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth.requestMatchers(AUTH_WHITELIST).permitAll()
                        .anyRequest().authenticated())
                .authenticationProvider(authenticationProvider(passwordEncoder))
                .exceptionHandling(ex -> ex.authenticationEntryPoint(unauthorizedHandler))
                .addFilterBefore(tokenFilter, UsernamePasswordAuthenticationFilter.class);

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
