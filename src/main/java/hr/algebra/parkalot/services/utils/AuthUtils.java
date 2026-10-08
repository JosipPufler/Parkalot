package hr.algebra.parkalot.services.utils;

import hr.algebra.parkalot.models.CustomUserDetails;
import hr.algebra.parkalot.models.entity.User;
import hr.algebra.parkalot.services.db.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
@RequiredArgsConstructor
public final class AuthUtils {
    private final UserService userService;

    public Optional<User> getCurrentUser(){
        var auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated() || !(auth.getPrincipal() instanceof CustomUserDetails))
            return Optional.empty();

        Long userId = ((CustomUserDetails) auth.getPrincipal()).getId();
        return userService.findById(userId);
    }
}
