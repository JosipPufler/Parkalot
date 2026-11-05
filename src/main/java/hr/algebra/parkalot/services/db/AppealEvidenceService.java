package hr.algebra.parkalot.services.db;

import hr.algebra.parkalot.models.entities.AppealEvidence;
import hr.algebra.parkalot.repositories.AppealEvidenceRepo;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class AppealEvidenceService extends GeneralCrudService<AppealEvidence, AppealEvidenceRepo> {
    protected AppealEvidenceService(AppealEvidenceRepo repository) {
        super(repository);
    }

    public List<AppealEvidence> findByAppealId(Long appealId){
        return repository.findByAppealId(appealId);
    }
}
