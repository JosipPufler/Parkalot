package hr.algebra.parkalot.repositories;

import hr.algebra.parkalot.models.entities.Permit;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface PermitRepo extends JpaRepository<Permit, Long> {
    List<Permit> findPermitsByVehicleLicencePlate(String licencePlate);
    Optional<Permit> findByPaypalId(String paypalId);
    List<Permit> findByPaymentTimeIsNullAndCreatedAtBefore(LocalDateTime cutoff);

    @Query("""
    SELECT COUNT(DISTINCT p.vehicle.id)
    FROM Permit p
    WHERE p.area.id = :areaId
      AND p.buyer.household.id = :householdId
      AND p.paymentTime IS NOT NULL
      AND p.startDate <= :today
      AND :today <= :endDate
    """)
    long countActivePermittedVehiclesByHouseholdAndArea(
            Long householdId,
            Long areaId,
            LocalDate today
    );
}
