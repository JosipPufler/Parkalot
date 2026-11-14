package hr.algebra.parkalot.controllers.rest;

import hr.algebra.parkalot.exceptions.LocalizedNotFoundException;
import hr.algebra.parkalot.models.entities.Area;
import hr.algebra.parkalot.models.dtos.AreaDto;
import hr.algebra.parkalot.models.dtos.CoordinateDto;
import hr.algebra.parkalot.models.enums.LocalizedExceptionKeys;
import hr.algebra.parkalot.services.db.AreaService;
import hr.algebra.parkalot.services.utils.HttpUtils;
import hr.algebra.parkalot.services.utils.Mapper;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.Optional;

@RestController
@RequestMapping("/rest/area")
@RequiredArgsConstructor
public class AreaController {
    private final AreaService areaService;

    @GetMapping("/whereami")
    @PreAuthorize("hasAnyRole('USER', 'OFFICER', 'ADMIN')")
    public ResponseEntity<?> getAreaForCoordinates(@RequestBody CoordinateDto coordinateDto){
        Optional<Area> currentArea = areaService.getCurrentArea(coordinateDto.latitude(), coordinateDto.longitude());
        if(currentArea.isEmpty()){
            return HttpUtils.produceNotFoundResponse("No area for the given coordinates: " + coordinateDto);
        }

        return ResponseEntity.ok(new AreaDto(currentArea.get()));
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('USER', 'OFFICER', 'ADMIN')")
    public ResponseEntity<?> getAllAreas() {
        return ResponseEntity.ok(areaService.findAll().stream().map(AreaDto::new).toList());
    }

    @PostMapping()
    @PreAuthorize("hasAnyRole('ADMIN')")
    public ResponseEntity<?> createArea(@Valid @RequestBody AreaDto areaDto) {
        Area area = areaService.saveArea(areaDto);

        return ResponseEntity.ok(new AreaDto(area));
    }

    @PutMapping()
    @PreAuthorize("hasAnyRole('ADMIN')")
    public ResponseEntity<?> updateArea(@Valid @RequestBody AreaDto areaDto) {
        Optional<Area> area = areaService.updateArea(areaDto);
        if (area.isEmpty()) {
            throw new LocalizedNotFoundException(LocalizedExceptionKeys.AREA_NOT_FOUND, areaDto.id());
        }

        return HttpUtils.produceOkResponse();
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN')")
    public ResponseEntity<?> deleteArea(@PathVariable Long id){
        if(!areaService.existsById(id)){
            throw new LocalizedNotFoundException(LocalizedExceptionKeys.AREA_NOT_FOUND, id);
        }
        areaService.deleteById(id);
        return HttpUtils.produceOkResponse();
    }
}
