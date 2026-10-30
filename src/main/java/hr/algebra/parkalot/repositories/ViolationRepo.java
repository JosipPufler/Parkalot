package hr.algebra.parkalot.repositories;

import hr.algebra.parkalot.models.entity.Violation;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ViolationRepo extends JpaRepository<Violation, Long> {
    List<Violation> findByParkingCheckLogLicencePlate(String licencePlate);
    Optional<Violation> findByOrderId(String orderId);
}
