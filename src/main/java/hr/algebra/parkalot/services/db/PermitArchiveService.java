package hr.algebra.parkalot.services.db;

import hr.algebra.parkalot.models.entity.PermitArchive;
import hr.algebra.parkalot.repositories.PermitArchiveRepo;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class PermitArchiveService extends GeneralCrudService<PermitArchive, PermitArchiveRepo> {
    protected PermitArchiveService(PermitArchiveRepo repository) {
        super(repository);
    }

    public List<PermitArchive> findByLicencePlate(String licencePlate) {
        return repository.findPermitArchiveByLicencePlate(licencePlate);
    }
}
