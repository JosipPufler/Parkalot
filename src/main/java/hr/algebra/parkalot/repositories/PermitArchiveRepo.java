package hr.algebra.parkalot.repositories;

import hr.algebra.parkalot.models.entity.PermitArchive;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PermitArchiveRepo extends JpaRepository<PermitArchive, Long> {
    List<PermitArchive> findPermitArchiveByLicencePlate(String licencePlate);
}
