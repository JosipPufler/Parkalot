package hr.algebra.parkalot.models.dtos;

import hr.algebra.parkalot.models.entities.Area;
import hr.algebra.parkalot.models.entities.PermitEligibilityRule;
import hr.algebra.parkalot.models.enums.Zone;
import jakarta.validation.constraints.Size;

import java.util.ArrayList;
import java.util.List;

public record AreaDto(Long id, String name, Zone zone, String zoneName, @Size(min = 3) List<CoordinateDto> coordinates,
                      EligibilityRuleDto rule) {
    public AreaDto(Area area) {
        List<CoordinateDto> coordinateDtos = new ArrayList<>(List.copyOf(area.getCoordinates()));
        coordinateDtos.removeLast();
        this(area.getId(), area.getName(), area.getZone(), area.getZone().getName(), coordinateDtos, new EligibilityRuleDto(area.getEligibilityRule()));
    }
}
