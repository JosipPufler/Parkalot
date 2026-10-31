package hr.algebra.parkalot.repositories;

import hr.algebra.parkalot.models.entity.Permit;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface PermitRepo extends JpaRepository<Permit, Long> {
    List<Permit> findPermitsByVehicleLicencePlate(String licencePlate);
    Optional<Permit> findByPaypalId(String paypalId);
    List<Permit> findByPaymentTimeIsNullAndCreatedAtBefore(LocalDateTime cutoff);
}
