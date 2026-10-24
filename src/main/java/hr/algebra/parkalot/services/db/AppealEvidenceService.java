package hr.algebra.parkalot.services.db;

import hr.algebra.parkalot.models.entity.AppealEvidence;
import hr.algebra.parkalot.repositories.AppealEvidenceRepo;
import org.springframework.stereotype.Service;

@Service
public class AppealEvidenceService extends GeneralCrudService<AppealEvidence, AppealEvidenceRepo> {
    protected AppealEvidenceService(AppealEvidenceRepo repository) {
        super(repository);
    }
}
