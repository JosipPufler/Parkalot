package hr.algebra.parkalot.services.db;

import hr.algebra.parkalot.models.entity.User;
import hr.algebra.parkalot.models.entity.Vehicle;
import hr.algebra.parkalot.repositories.VehicleRepo;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class VehicleService extends GeneralCrudService<Vehicle, VehicleRepo> {
    protected VehicleService(VehicleRepo repository) {
        super(repository);
    }

    public List<Vehicle> findByUser(User user) {
        return repository.findByUser(user);
    }

    public Optional<Vehicle> findByLicencePlate(String licencePlate) {
        return repository.findByLicencePlate(licencePlate);
    }
}
