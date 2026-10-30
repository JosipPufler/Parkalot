package hr.algebra.parkalot.services.db;

import hr.algebra.parkalot.models.entity.ParkingCheckLog;
import hr.algebra.parkalot.models.entity.Violation;
import hr.algebra.parkalot.repositories.ViolationRepo;
import jakarta.transaction.Transactional;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
public class ViolationService extends GeneralCrudService<Violation, ViolationRepo> {
    private final ParkingCheckLogService parkingCheckLogService;

    public static final Double TICKET_PRICE = 8.99;

    protected ViolationService(ViolationRepo repository, ParkingCheckLogService parkingCheckLogService) {
        super(repository);
        this.parkingCheckLogService = parkingCheckLogService;
    }

    @Transactional
    public Violation issueViolation(ParkingCheckLog parkingCheckLog){
        Violation violation = save(Violation.builder()
                .active(Boolean.TRUE)
                .message("")
                .timestamp(LocalDateTime.now())
                .price(TICKET_PRICE)
                .parkingCheckLog(parkingCheckLog)
                .build());

        parkingCheckLog.setViolation(violation);
        parkingCheckLogService.save(parkingCheckLog);

        return violation;
    }

    public List<Violation> findViolationsByLicencePlate(String licencePlate){
        return repository.findByParkingCheckLogLicencePlate(licencePlate);
    }

    public Optional<Violation> findByOrderId(String orderId){
        return repository.findByOrderId(orderId);
    }
}
