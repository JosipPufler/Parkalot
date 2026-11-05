package hr.algebra.parkalot.services.db;

import hr.algebra.parkalot.models.enums.PermitDuration;
import hr.algebra.parkalot.models.dtos.AreaDto;
import hr.algebra.parkalot.models.entities.Area;
import hr.algebra.parkalot.repositories.AreaRepo;
import hr.algebra.parkalot.services.utils.Mapper;
import jakarta.transaction.Transactional;
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

    public Optional<Double> calculateAreaPermitPrice(Long areaId, PermitDuration permitDuration) {
        return repository.findById(areaId).map(value -> value.getZone().getPrice() * permitDuration.getMultiplier());
    }

    @Transactional
    public Optional<Area> updateArea(AreaDto areaDto){
        Optional<Area> areaOpt = findById(areaDto.id());
        if (areaOpt.isEmpty())
            return areaOpt;

        Area newArea = Mapper.mapAreaDtoToArea(areaDto);
        Area area = areaOpt.get();
        area.setName(newArea.getName());
        area.setZone(newArea.getZone());
        area.setAreaPolygon(newArea.getAreaPolygon());
        return Optional.of(save(area));
    }
}
