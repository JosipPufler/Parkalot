package hr.algebra.parkalot.repositories;

import hr.algebra.parkalot.models.entity.Permit;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PermitRepo extends JpaRepository<Permit, Long> {
    List<Permit> findPermitsByVehicleLicencePlate(String licencePlate);
}
