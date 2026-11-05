package hr.algebra.parkalot.services.db;

import hr.algebra.parkalot.models.entities.User;
import hr.algebra.parkalot.repositories.UserRepo;
import org.springframework.stereotype.Service;

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
}
