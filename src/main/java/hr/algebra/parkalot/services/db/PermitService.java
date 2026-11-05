package hr.algebra.parkalot.services.db;

import hr.algebra.parkalot.models.dtos.PermitUpdateForm;
import hr.algebra.parkalot.models.entities.Area;
import hr.algebra.parkalot.models.entities.Permit;
import hr.algebra.parkalot.models.entities.PermitArchive;
import hr.algebra.parkalot.models.entities.Vehicle;
import hr.algebra.parkalot.repositories.PermitRepo;
import jakarta.persistence.EntityNotFoundException;
import jakarta.transaction.Transactional;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
public class PermitService extends GeneralCrudService<Permit, PermitRepo> {
    private final PermitArchiveService permitArchiveService;
    private final AreaService areaService;
    private final VehicleService vehicleService;

    protected PermitService(PermitRepo repository, PermitArchiveService permitArchiveService, AreaService areaService, VehicleService vehicleService) {
        super(repository);
        this.permitArchiveService = permitArchiveService;
        this.areaService = areaService;
        this.vehicleService = vehicleService;
    }

    public List<Permit> getPermitsByLicencePlate(String licencePlate) {
        return repository.findPermitsByVehicleLicencePlate(licencePlate);
    }

    public Boolean permitsConflict(Permit permit) {
        return permit.getVehicle().getPermits()
                .stream().filter(x -> x.getArea() == permit.getArea())
                .anyMatch(p -> p.overlaps(permit.getStartDate(), permit.getPermitDuration().calculateEndDate
                        .apply(permit.getStartDate())));
    }

    public Optional<Permit> findByOrderId(String orderId) {
        return repository.findByPaypalId(orderId);
    }

    public Permit updatePermit(PermitUpdateForm permitDto) {
        Permit permit = findById(permitDto.id()).orElseThrow(() ->
                new EntityNotFoundException("Permit with ID: " + permitDto.id() + " not found"));
        Area area = areaService.findById(permitDto.areaId()).orElseThrow(() ->
                new EntityNotFoundException("Area with ID: " + permitDto.areaId() + " not found"));
        Vehicle vehicle = vehicleService.findById(permitDto.vehicleId()).orElseThrow(() ->
                new EntityNotFoundException("Vehicle with ID: " + permitDto.vehicleId() + " not found"));

        permit.setPermitDuration(permitDto.permitDuration());
        permit.setStartDate(permitDto.startDate());
        permit.setArea(area);
        permit.setVehicle(vehicle);

        return repository.save(permit);
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
