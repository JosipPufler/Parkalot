package hr.algebra.parkalot.services.db;

import hr.algebra.parkalot.models.entity.Appeal;
import hr.algebra.parkalot.models.entity.AppealEvidence;
import hr.algebra.parkalot.repositories.AppealEvidenceRepo;
import hr.algebra.parkalot.repositories.AppealRepo;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

@Service
public class AppealService extends GeneralCrudService<Appeal, AppealRepo> {
    private final AppealEvidenceService appealEvidenceService;
    private final ViolationService violationService;

    protected AppealService(AppealRepo repository, AppealEvidenceService appealEvidenceService, ViolationService violationService) {
        super(repository);
        this.appealEvidenceService = appealEvidenceService;
        this.violationService = violationService;
    }

    public List<AppealEvidence> constructAppealEvidence(Appeal appeal, List<MultipartFile> files) throws IOException {
        List<AppealEvidence> evidence = new ArrayList<>();
        for (MultipartFile file : files) {
            AppealEvidence appealEvidence = AppealEvidence.builder()
                    .appeal(appeal)
                    .mediaType(file.getContentType())
                    .filename(file.getOriginalFilename())
                    .evidence(file.getBytes())
                    .build();
            evidence.add(appealEvidenceService.save(appealEvidence));
        }
        return evidence;
    }

    public List<Appeal> findByUserId(Long userId) {
        return repository.findByUserId(userId);
    }

    public List<Appeal> findByOfficerId(Long officerId) {
        return repository.findByViolation_ParkingCheckLog_Officer_Id(officerId);
    }

    public void rejectAppeal(Appeal appeal) {
        appeal.setRejected(Boolean.TRUE);
        appeal.setResolved(Boolean.TRUE);
        repository.save(appeal);
    }

    public void approveAppeal(Appeal appeal) {
        appeal.setResolved(Boolean.TRUE);
        appeal.getViolation().setActive(Boolean.FALSE);
        violationService.save(appeal.getViolation());
        repository.save(appeal);
    }
}
