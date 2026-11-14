package hr.algebra.parkalot.services.db;

import hr.algebra.parkalot.exceptions.LocalizedNotFoundException;
import hr.algebra.parkalot.models.dtos.PermitUpdateForm;
import hr.algebra.parkalot.models.entities.*;
import hr.algebra.parkalot.models.enums.LocalizedExceptionKeys;
import hr.algebra.parkalot.repositories.PermitRepo;
import jakarta.transaction.Transactional;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Comparator;
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
                new LocalizedNotFoundException(LocalizedExceptionKeys.PERMIT_NOT_FOUND, permitDto.id()));
        Area area = areaService.findById(permitDto.areaId()).orElseThrow(() ->
                new LocalizedNotFoundException(LocalizedExceptionKeys.AREA_NOT_FOUND, permitDto.areaId()));
        Vehicle vehicle = vehicleService.findById(permitDto.vehicleId()).orElseThrow(() ->
                new LocalizedNotFoundException(LocalizedExceptionKeys.VEHICLE_NOT_FOUND, permitDto.vehicleId()));

        permit.setPermitDuration(permitDto.permitDuration());
        permit.setStartDate(permitDto.startDate());
        permit.setArea(area);
        permit.setVehicle(vehicle);
        User buyer = vehicle.getOwnerships().stream()
                .filter(VehicleOwnership::getPrimaryOwner)
                .map(VehicleOwnership::getUser)
                .findFirst()
                .orElseThrow(() ->
                        new LocalizedNotFoundException(LocalizedExceptionKeys.VEHICLE_NO_OWNERS));

        permit.setBuyer(buyer);
        
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
