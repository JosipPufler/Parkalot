package hr.algebra.parkalot.services.db;

import hr.algebra.parkalot.models.PermitDuration;
import hr.algebra.parkalot.models.Zone;
import hr.algebra.parkalot.models.entity.Permit;
import hr.algebra.parkalot.repositories.PermitRepo;
import jakarta.transaction.Transactional;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Service
public class PermitService extends GeneralCrudService<Permit, PermitRepo> {
    protected PermitService(PermitRepo repository) {
        super(repository);
    }

    public List<Permit> getPermitsByLicencePlate(String licencePlate) {
        return repository.findPermitsByVehicleLicencePlate(licencePlate);
    }

    @Transactional
    public Optional<Permit> tryBuyPermit(Permit permit){
        return permitsConflict(
                permit.getVehicle().getPermits(), permit.getZone(), permit.getStartDate(), permit.getPermitDuration())
                    ? Optional.empty() : Optional.of(save(permit));
    }

    public Boolean permitsConflict(List<Permit> permits, Zone zone, LocalDate startDate, PermitDuration permitDuration) {
        return permits.stream().filter(x -> x.getZone() == zone)
                .anyMatch(permit -> permit.overlaps(startDate, permitDuration.calculateEndDate.apply(startDate)));
    }
}
