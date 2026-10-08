package hr.algebra.parkalot.services;

import hr.algebra.parkalot.models.entity.Permit;
import hr.algebra.parkalot.models.entity.PermitArchive;
import hr.algebra.parkalot.models.Zone;
import hr.algebra.parkalot.services.db.AreaService;
import hr.algebra.parkalot.services.db.PermitArchiveService;
import hr.algebra.parkalot.services.db.PermitService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class ParkingService {
    private final PermitService permitService;
    private final PermitArchiveService permitArchiveService;
    private final AreaService areaService;

    public Optional<Permit> checkParkingStatusIsValid(String licencePlateNumber, Zone currentZone) {
        return cullPermits(permitService.getPermitsByLicencePlate(licencePlateNumber))
                .stream().filter(permit -> permit.getZone() == currentZone && permit.isActive()).findFirst();
    }

    public void cullAllPermits(){
        cullPermits(permitService.findAll());
    }

    private List<Permit> cullPermits(List<Permit> permits){
        for(int i = permits.size() - 1 ; i >= 0 ; i--){
            if (permits.get(i).isExpired()) {
                permitArchiveService.save(new PermitArchive(permits.get(i)));
                permitService.delete(permits.get(i));
                permits.remove(i);
            }
        }

        return permits;
    }
}
