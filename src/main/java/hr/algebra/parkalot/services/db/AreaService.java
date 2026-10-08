package hr.algebra.parkalot.services.db;

import hr.algebra.parkalot.models.entity.Area;
import hr.algebra.parkalot.repositories.AreaRepo;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
@Slf4j
public class AreaService extends GeneralCrudService<Area, AreaRepo> {
    protected AreaService(AreaRepo repository) {
        super(repository);
    }

    public Optional<Area> getCurrentArea(Double lat, Double lng){
        List<Area> areasByCoordinates = repository.findAreasByCoordinates(lat, lng);
        if (areasByCoordinates.isEmpty()){
            log.warn("No areas found for coordinates {}, {}", lat, lng);
            return Optional.empty();
        }

        if (areasByCoordinates.size() > 1){
            log.warn("Multiple areas found for coordinates {}, {}", lat, lng);
        }

        return Optional.of(areasByCoordinates.getFirst());
    }
}
