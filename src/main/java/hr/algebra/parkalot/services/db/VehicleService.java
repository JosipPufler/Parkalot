package hr.algebra.parkalot.services.db;

import hr.algebra.parkalot.models.dtos.VehicleDto;
import hr.algebra.parkalot.models.entities.User;
import hr.algebra.parkalot.models.entities.Vehicle;
import hr.algebra.parkalot.repositories.VehicleRepo;
import hr.algebra.parkalot.services.utils.Mapper;
import jakarta.persistence.EntityNotFoundException;
import jakarta.transaction.Transactional;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class VehicleService extends GeneralCrudService<Vehicle, VehicleRepo> {
    private final UserService userService;
    protected VehicleService(VehicleRepo repository, UserService userService) {
        super(repository);
        this.userService = userService;
    }

    @Transactional
    public Vehicle createVehicle(VehicleDto vehicleDto) {
        Vehicle vehicle = Mapper.mapVehicleDtoToVehicle(vehicleDto);

        vehicleDto.ownerIds().forEach(id -> {
            User user = userService.findById(id).orElseThrow(() ->
                    new EntityNotFoundException("User with ID: " + id + " not found"));
            user.addVehicle(vehicle);
        });

        return repository.save(vehicle);
    }

    public Vehicle registerVehicle(VehicleDto vehicleDto, User user) {
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

    @Transactional
    public Vehicle updateVehicle(VehicleDto vehicleDto) {
        Vehicle vehicle = findById(vehicleDto.id()).orElseThrow(() ->
                new EntityNotFoundException(
                        "Vehicle with ID: " + vehicleDto.id() + " not found"));

        List<User> newOwners = vehicleDto.ownerIds().stream()
                .distinct()
                .map(id -> userService.findById(id).orElseThrow(() ->
                        new EntityNotFoundException("User with ID: " + id + " not found")))
                .toList();

        Set<Long> newOwnerIds = newOwners.stream()
                .map(User::getId)
                .collect(Collectors.toSet());

        for (User currentOwner : new ArrayList<>(vehicle.getOwners())) {
            if (!newOwnerIds.contains(currentOwner.getId())) {
                currentOwner.removeVehicle(vehicle);
            }
        }

        Set<Long> currentOwnerIds = vehicle.getOwners().stream()
                .map(User::getId)
                .collect(Collectors.toSet());

        for (User newOwner : newOwners) {
            if (!currentOwnerIds.contains(newOwner.getId())) {
                newOwner.addVehicle(vehicle);
            }
        }

        vehicle.setLicencePlate(vehicleDto.licencePlate());
        vehicle.setMake(vehicleDto.make());
        vehicle.setModel(vehicleDto.model());
        vehicle.setYear(vehicleDto.year());

        return save(vehicle);
    }
}
