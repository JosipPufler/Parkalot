package hr.algebra.parkalot.services.db;

import hr.algebra.parkalot.models.entity.Appeal;
import hr.algebra.parkalot.repositories.AppealRepo;
import org.springframework.stereotype.Service;

@Service
public class AppealService extends GeneralCrudService<Appeal, AppealRepo> {
    protected AppealService(AppealRepo repository) {
        super(repository);
    }
}
