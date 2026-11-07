package hr.algebra.parkalot.services.db;

import hr.algebra.parkalot.exceptions.DbConflictException;
import hr.algebra.parkalot.models.dtos.UserDto;
import hr.algebra.parkalot.models.entities.User;
import hr.algebra.parkalot.repositories.UserRepo;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.stereotype.Service;

import java.util.Objects;
import java.util.Optional;

@Service
public class UserService extends GeneralCrudService<User, UserRepo> {
    protected UserService(UserRepo repository) {
        super(repository);
    }

    public Boolean existsByEmail(String email) {
        return repository.existsByEmail(email);
    }

    public Optional<User> findByEmail(String email) {
        return repository.findByEmail(email);
    }

    public User updateUser(UserDto userDto) {
        User user = findById(userDto.id()).orElseThrow(() ->
                new EntityNotFoundException("User with ID: " + userDto.id() + " not found"));

        if (!Objects.equals(user.getEmail(), userDto.email()) && existsByEmail(userDto.email())) {
            throw new DbConflictException("Duplicate email detected: " + userDto.email()
                    + " while updating user with id: " + userDto.id());
        }

        user.setFirstName(userDto.firstName());
        user.setLastName(userDto.lastName());
        user.setEmail(userDto.email());
        user.setRole(userDto.role());
        return repository.save(user);
    }
}
