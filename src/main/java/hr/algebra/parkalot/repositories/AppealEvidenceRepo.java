package hr.algebra.parkalot.repositories;

import hr.algebra.parkalot.models.entities.AppealEvidence;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface AppealEvidenceRepo extends JpaRepository<AppealEvidence, Long> {
    List<AppealEvidence> findByAppealId(Long appealId);
}
