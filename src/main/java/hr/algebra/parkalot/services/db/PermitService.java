package hr.algebra.parkalot.services.db;

import hr.algebra.parkalot.models.PermitDuration;
import hr.algebra.parkalot.models.entity.Area;
import hr.algebra.parkalot.models.entity.Permit;
import hr.algebra.parkalot.models.entity.PermitArchive;
import hr.algebra.parkalot.repositories.PermitRepo;
import jakarta.transaction.Transactional;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;

@Service
public class PermitService extends GeneralCrudService<Permit, PermitRepo> {
    private final PermitArchiveService permitArchiveService;

    protected PermitService(PermitRepo repository, PermitArchiveService permitArchiveService) {
        super(repository);
        this.permitArchiveService = permitArchiveService;
    }

    public List<Permit> getPermitsByLicencePlate(String licencePlate) {
        return repository.findPermitsByVehicleLicencePlate(licencePlate);
    }

    @Transactional
    public Optional<Permit> tryBuyPermit(Permit permit){
        return permitsConflict(
                permit.getVehicle().getPermits(), permit.getArea(), permit.getStartDate(), permit.getPermitDuration())
                    ? Optional.empty() : Optional.of(save(permit));
    }

    public Boolean permitsConflict(List<Permit> permits, Area area, LocalDate startDate, PermitDuration permitDuration) {
        return permits.stream().filter(x -> x.getArea() == area)
                .anyMatch(permit -> permit.overlaps(startDate, permitDuration.calculateEndDate.apply(startDate)));
    }

    public Optional<Permit> findByOrderId(String orderId) {
        return repository.findByPaypalId(orderId);
    }

    @Transactional
    public void cleanUpExpired(){
        findAll().forEach(permit -> {
            if (permit.isExpired()){
                permitArchiveService.save(new PermitArchive(permit));
                delete(permit);
            }
        });
    }

    @Transactional
    public void cleanUpUnpaid() {
        LocalDateTime cutoff = LocalDateTime.now().minusMinutes(30);

        List<Permit> unpaidPermits =
                repository.findByPaymentTimeIsNullAndCreatedAtBefore(cutoff);

        repository.deleteAllInBatch(unpaidPermits);
    }
}
