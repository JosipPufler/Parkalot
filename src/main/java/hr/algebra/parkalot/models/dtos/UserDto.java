package hr.algebra.parkalot.models.dtos;

import hr.algebra.parkalot.models.entities.User;
import hr.algebra.parkalot.models.enums.Role;

public record UserDto(Long id, String firstName, String lastName, String email, Role role) {
    public UserDto(User user) {
        this(user.getId(), user.getFirstName(), user.getLastName(), user.getEmail(), user.getRole());
    }
}
