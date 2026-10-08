package hr.algebra.parkalot.repositories;

import hr.algebra.parkalot.models.entity.Violation;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ViolationRepo extends JpaRepository<Violation, Long> {
    List<Violation> findByLicencePlate(String licencePlate);
}
