package hr.algebra.parkalot.repositories;

import hr.algebra.parkalot.models.entity.Appeal;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface AppealRepo extends JpaRepository<Appeal, Long> {
}
