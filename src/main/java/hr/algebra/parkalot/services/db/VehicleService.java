package hr.algebra.parkalot.services.db;

import hr.algebra.parkalot.models.dtos.VehicleDto;
import hr.algebra.parkalot.models.dtos.VehicleUpdateForm;
import hr.algebra.parkalot.models.entities.User;
import hr.algebra.parkalot.models.entities.Vehicle;
import hr.algebra.parkalot.repositories.VehicleRepo;
import hr.algebra.parkalot.services.utils.Mapper;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
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

    public Vehicle updateVehicle(VehicleUpdateForm vehicleDto){
        Vehicle vehicle = findById(vehicleDto.id()).orElseThrow(() ->
                new EntityNotFoundException("Vehicle with ID: " + vehicleDto.id() + " not found"));

        List<User> owners = new ArrayList<>();
        vehicleDto.ownerIds().forEach(id -> {
            User user = userService.findById(id).orElseThrow(() ->
                    new EntityNotFoundException("User with ID: " + id + " not found"));

            user.addVehicle(vehicle);
        });

        vehicle.setLicencePlate(vehicleDto.licencePlate());
        vehicle.setMake(vehicleDto.make());
        vehicle.setModel(vehicleDto.model());
        vehicle.setYear(vehicleDto.year());

        return save(vehicle);
    }
}
