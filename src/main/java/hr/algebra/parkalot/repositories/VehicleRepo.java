package hr.algebra.parkalot.repositories;

import hr.algebra.parkalot.models.entity.User;
import hr.algebra.parkalot.models.entity.Vehicle;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface VehicleRepo extends JpaRepository<Vehicle, Long> {
     List<Vehicle> findByOwner(User user);
     Optional<Vehicle> findByLicencePlate(String plateNumber);
}
