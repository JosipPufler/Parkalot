package hr.algebra.parkalot.helper;

import hr.algebra.parkalot.models.enums.Role;
import hr.algebra.parkalot.models.entities.User;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class UserHelper {
    static BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();

    public static User getAdmin() {
        return User.builder()
                .firstName("Adam")
                .lastName("Adminović")
                .password(encoder.encode("pass"))
                .email("admin@example.com")
                .role(Role.ADMIN)
                .build();
    }

    public static User getOfficer() {
        return User.builder()
                .firstName("Oleg")
                .lastName("Officirović")
                .password(encoder.encode("pass"))
                .email("officer@example.com")
                .role(Role.OFFICER)
                .build();
    }

    public static User getUser() {
        return User.builder()
                .firstName("Karlo")
                .lastName("Korisnić")
                .password(encoder.encode("pass"))
                .email("user@example.com")
                .role(Role.USER)
                .build();
    }
}
