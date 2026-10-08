package hr.algebra.parkalot.repositories;

import hr.algebra.parkalot.models.entity.ParkingCheckLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ParkingCheckLogRepo extends JpaRepository<ParkingCheckLog, Long> {
}
