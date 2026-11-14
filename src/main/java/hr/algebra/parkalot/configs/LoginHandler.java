package hr.algebra.parkalot.configs;

import hr.algebra.parkalot.models.entities.User;
import hr.algebra.parkalot.services.JwtService;
import hr.algebra.parkalot.services.db.UserService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.http.ResponseCookie;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.Optional;

@Component
@RequiredArgsConstructor
public class LoginHandler implements AuthenticationSuccessHandler {
    private final JwtService jwtService;
    private final UserService userService;
    private final ApplicationEventPublisher publisher;

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request,
                                        HttpServletResponse response,
                                        Authentication authentication) throws IOException {
        String email = authentication.getName();

        Optional<User> byEmail = userService.findByEmail(email);
        if (byEmail.isEmpty()) {
            response.sendRedirect("/mvc/public/auth/login");
            return;
        }

        ResponseCookie access = jwtService.generateAccessToken(byEmail.get());

        response.addHeader("Set-Cookie", access.toString());

        response.sendRedirect("/mvc/map");
    }
}
