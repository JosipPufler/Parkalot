package hr.algebra.parkalot.repositories;

import hr.algebra.parkalot.models.entities.Permit;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface PermitRepo extends JpaRepository<Permit, Long> {
    @EntityGraph(attributePaths = {
            "vehicle",
            "area",
            "buyer"
    })
    List<Permit> findPermitsByVehicleLicencePlate(String licencePlate);
    Optional<Permit> findByPaypalId(String paypalId);
    List<Permit> findByPaymentTimeIsNullAndCreatedAtBefore(LocalDateTime cutoff);

    @Query("""
    SELECT p
    FROM Permit p
    WHERE p.area.id = :areaId
      AND p.buyer.household.id = :householdId
      AND p.paymentTime IS NOT NULL
      AND p.startDate <= :today
    """)
    List<Permit> findStartedPaidPermitsByHouseholdAndArea(
            Long householdId,
            Long areaId,
            LocalDate today
    );
}
