package hr.algebra.parkalot.services.db;

import hr.algebra.parkalot.exceptions.LocalizedConflictException;
import hr.algebra.parkalot.models.entities.PermitEligibilityRule;
import hr.algebra.parkalot.models.enums.LocalizedExceptionKeys;
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
    public Area saveArea(AreaDto areaDto){
        Area area = Mapper.mapAreaDtoToArea(areaDto);
        if (repository.overlapsExistingArea(
                area.getAreaPolygon(),
                areaDto.id())) {
            throw new LocalizedConflictException(LocalizedExceptionKeys.AREA_OVERLAPPING);
        }

        PermitEligibilityRule permitEligibilityRule = Mapper.mapPermitEligibilityRuleFromDto(areaDto.rule());
        area.setEligibilityRule(permitEligibilityRule);
        return save(area);
    }

    @Transactional
    public Optional<Area> updateArea(AreaDto areaDto){
        Optional<Area> areaOpt = findById(areaDto.id());
        if (areaOpt.isEmpty())
            return areaOpt;

        Area newArea = Mapper.mapAreaDtoToArea(areaDto);
        if (repository.overlapsExistingArea(
                newArea.getAreaPolygon(),
                areaDto.id())) {
            throw new LocalizedConflictException(LocalizedExceptionKeys.AREA_OVERLAPPING);
        }

        Area area = areaOpt.get();
        area.setName(newArea.getName());
        area.setZone(newArea.getZone());
        area.setAreaPolygon(newArea.getAreaPolygon());

        area.getEligibilityRule().setMaxVehiclesPerHousehold(areaDto.rule().maxVehiclesPerHousehold());
        area.getEligibilityRule().setMinimumEmissionClass(areaDto.rule().emissionClass());
        area.getEligibilityRule().setRequiresResidentAddress(areaDto.rule().requiresResidentAddress());
        return Optional.of(save(area));
    }
}
