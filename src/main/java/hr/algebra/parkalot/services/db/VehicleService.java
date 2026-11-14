
package hr.algebra.parkalot.services.db;

import hr.algebra.parkalot.exceptions.LocalizedNotFoundException;
import hr.algebra.parkalot.models.dtos.VehicleDto;
import hr.algebra.parkalot.models.entities.User;
import hr.algebra.parkalot.models.entities.Vehicle;
import hr.algebra.parkalot.models.entities.VehicleOwnership;
import hr.algebra.parkalot.models.enums.LocalizedExceptionKeys;
import hr.algebra.parkalot.repositories.VehicleRepo;
import hr.algebra.parkalot.services.utils.Mapper;
import jakarta.transaction.Transactional;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
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

        List<User> owners = vehicleDto.ownerIds().stream()
                .distinct()
                .map(id -> userService.findById(id).orElseThrow(() ->
                        new LocalizedNotFoundException(
                                LocalizedExceptionKeys.USER_NOT_FOUND, id)))
                .toList();

        for (int i = 0; i < owners.size(); i++) {
            addOwnership(vehicle, owners.get(i), i == 0);
        }

        return repository.save(vehicle);
    }

    @Transactional
    public Vehicle registerVehicle(VehicleDto vehicleDto, User user) {
        Vehicle vehicle = Mapper.mapVehicleDtoToVehicle(vehicleDto);

        addOwnership(vehicle, user, true);

        return repository.save(vehicle);
    }

    public List<Vehicle> findByOwner(User user) {
        return repository.findDistinctByOwnerships_User(user);
    }

    public Optional<Vehicle> findByLicencePlate(String licencePlate) {
        return repository.findByLicencePlate(licencePlate);
    }

    @Transactional
    public Vehicle updateVehicle(VehicleDto vehicleDto) {
        Vehicle vehicle = findById(vehicleDto.id()).orElseThrow(() ->
                new LocalizedNotFoundException(
                        LocalizedExceptionKeys.VEHICLE_NOT_FOUND,
                        vehicleDto.id()));

        List<User> newOwners = vehicleDto.ownerIds().stream()
                .distinct()
                .map(id -> userService.findById(id).orElseThrow(() ->
                        new LocalizedNotFoundException(
                                LocalizedExceptionKeys.USER_NOT_FOUND, id)))
                .toList();

        Set<Long> newOwnerIds = newOwners.stream()
                .map(User::getId)
                .collect(Collectors.toSet());

        List<VehicleOwnership> currentOwnerships =
                new ArrayList<>(vehicle.getOwnerships());

        // Preserve the current primary owner if they are still selected.
        Long primaryOwnerId = currentOwnerships.stream()
                .filter(ownership -> ownership.getOwnershipEndDate() == null)
                .filter(ownership -> Boolean.TRUE.equals(ownership.getPrimaryOwner()))
                .map(ownership -> ownership.getUser().getId())
                .filter(newOwnerIds::contains)
                .findFirst()
                .orElse(newOwners.isEmpty() ? null : newOwners.get(0).getId());

        // Remove ownership relationships that are no longer selected.
        for (VehicleOwnership ownership : currentOwnerships) {
            if (!newOwnerIds.contains(ownership.getUser().getId())) {
                ownership.getUser().getVehicleOwnerships().remove(ownership);
                vehicle.getOwnerships().remove(ownership);
            }
        }

        // Add new ownership relationships.
        Set<Long> currentOwnerIds = vehicle.getOwnerships().stream()
                .filter(ownership -> ownership.getOwnershipEndDate() == null)
                .map(ownership -> ownership.getUser().getId())
                .collect(Collectors.toSet());

        for (User newOwner : newOwners) {
            if (!currentOwnerIds.contains(newOwner.getId())) {
                addOwnership(
                        vehicle,
                        newOwner,
                        newOwner.getId().equals(primaryOwnerId)
                );
            }
        }

        // Ensure exactly one selected owner is primary.
        vehicle.getOwnerships().stream()
                .filter(ownership -> ownership.getOwnershipEndDate() == null)
                .forEach(ownership ->
                        ownership.setPrimaryOwner(
                                ownership.getUser().getId().equals(primaryOwnerId)
                        ));

        vehicle.setLicencePlate(vehicleDto.licencePlate());
        vehicle.setMake(vehicleDto.make());
        vehicle.setModel(vehicleDto.model());
        vehicle.setYear(vehicleDto.year());

        return repository.save(vehicle);
    }

    private void addOwnership(Vehicle vehicle, User user, boolean primaryOwner) {
        VehicleOwnership ownership = new VehicleOwnership();

        ownership.setUser(user);
        ownership.setVehicle(vehicle);
        ownership.setOwnershipStartDate(LocalDate.now());
        ownership.setOwnershipEndDate(null);
        ownership.setPrimaryOwner(primaryOwner);

        vehicle.getOwnerships().add(ownership);
        user.getVehicleOwnerships().add(ownership);
    }
}
