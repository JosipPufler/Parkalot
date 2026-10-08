package hr.algebra.parkalot.services.db;

import hr.algebra.parkalot.models.entity.Area;
import hr.algebra.parkalot.models.entity.ParkingCheckLog;
import hr.algebra.parkalot.models.entity.User;
import hr.algebra.parkalot.models.entity.Violation;
import hr.algebra.parkalot.repositories.ViolationRepo;
import jakarta.transaction.Transactional;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class ViolationService extends GeneralCrudService<Violation, ViolationRepo> {
    public static final Double TICKET_PRICE = 8.99;

    protected ViolationService(ViolationRepo repository) {
        super(repository);
    }

    @Transactional
    public Violation issueViolation(User officer, Area area, String licencePlate, ParkingCheckLog parkingCheckLog){
        return save(Violation.builder()
                .area(area)
                .licencePlate(licencePlate)
                .officer(officer)
                .timestamp(LocalDateTime.now())
                .price(TICKET_PRICE)
                .parkingCheckLog(parkingCheckLog)
                .build());
    }

    public List<Violation> findViolationsByLicencePlate(String licencePlate){
        return repository.findByLicencePlate(licencePlate);
    }
}
