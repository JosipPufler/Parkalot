package hr.algebra.parkalot.repositories;

import hr.algebra.parkalot.models.entities.User;
import hr.algebra.parkalot.models.entities.Vehicle;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface VehicleRepo extends JpaRepository<Vehicle, Long> {
     List<Vehicle> findByOwnersContaining(User owner);
     Optional<Vehicle> findByLicencePlate(String plateNumber);
}
