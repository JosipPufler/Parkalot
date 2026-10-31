package hr.algebra.parkalot.helper;

import hr.algebra.parkalot.models.entity.Appeal;
import hr.algebra.parkalot.models.entity.User;
import hr.algebra.parkalot.models.entity.Violation;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class AppealHelper {
    public static Appeal getAppeal(Violation violation, User user) {
        return Appeal.builder()
                .user(user)
                .violation(violation)
                .createdAt(LocalDateTime.now())
                .deadline(LocalDate.now().plusDays(1))
                .title("This wrong")
                .description("This wrong!")
                .rejected(false)
                .resolved(false)
                .build();
    }
}
