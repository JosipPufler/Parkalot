package hr.algebra.parkalot.services.db;

import hr.algebra.parkalot.models.dtos.VehicleDto;
import hr.algebra.parkalot.models.entities.User;
import hr.algebra.parkalot.models.entities.Vehicle;
import hr.algebra.parkalot.repositories.VehicleRepo;
import hr.algebra.parkalot.services.utils.Mapper;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class VehicleService extends GeneralCrudService<Vehicle, VehicleRepo> {
    private final UserService userService;
    protected VehicleService(VehicleRepo repository, UserService userService) {
        super(repository);
        this.userService = userService;
    }

    public Vehicle createVehicleFromDto(VehicleDto vehicleDto, User user) {
        Vehicle vehicle = Mapper.mapVehicleDtoToVehicle(vehicleDto);

        repository.save(vehicle);
        user.addVehicle(vehicle);
        userService.save(user);

        return vehicle;
    }

    public List<Vehicle> findByOwner(User user) {
        return repository.findByOwnersContaining(user);
    }

    public Optional<Vehicle> findByLicencePlate(String licencePlate) {
        return repository.findByLicencePlate(licencePlate);
    }
}
