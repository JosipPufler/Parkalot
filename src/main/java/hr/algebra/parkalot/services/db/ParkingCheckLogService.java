package hr.algebra.parkalot.services.db;

import hr.algebra.parkalot.models.entity.ParkingCheckLog;
import hr.algebra.parkalot.repositories.ParkingCheckLogRepo;
import org.springframework.stereotype.Service;

@Service
public class ParkingCheckLogService extends GeneralCrudService<ParkingCheckLog, ParkingCheckLogRepo> {
    protected ParkingCheckLogService(ParkingCheckLogRepo repository) {
        super(repository);
    }
}
