package hr.algebra.parkalot.repositories;

import hr.algebra.parkalot.models.entity.Appeal;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface AppealRepo extends JpaRepository<Appeal, Long> {
    List<Appeal> findByUserId(Long userId);
    List<Appeal> findByViolation_ParkingCheckLog_Officer_Id(Long officerId);
}
