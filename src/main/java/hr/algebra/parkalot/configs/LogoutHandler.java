package hr.algebra.parkalot.configs;

import hr.algebra.parkalot.services.JwtService;
import hr.algebra.parkalot.services.db.UserService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseCookie;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.logout.LogoutSuccessHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Component
@RequiredArgsConstructor
public class LogoutHandler implements LogoutSuccessHandler {
    private final JwtService jwtService;
    private final UserService userService;

    @Override
    public void onLogoutSuccess(HttpServletRequest request, HttpServletResponse response, Authentication authentication) throws IOException {
        ResponseCookie accessCookie = ResponseCookie.from("JWT_TOKEN", "")
                .path("/")
                .maxAge(0)
                .httpOnly(true)
                .secure(true)
                .build();

        response.addHeader("Set-Cookie", accessCookie.toString());

        response.sendRedirect("/auth/login");
    }
}
