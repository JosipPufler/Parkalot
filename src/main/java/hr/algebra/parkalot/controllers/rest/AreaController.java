package hr.algebra.parkalot.controllers.rest;

import hr.algebra.parkalot.models.entity.Area;
import hr.algebra.parkalot.models.dto.AreaDto;
import hr.algebra.parkalot.models.dto.CoordinateDto;
import hr.algebra.parkalot.services.db.AreaService;
import hr.algebra.parkalot.services.utils.HttpUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.data.geo.Point;
import org.springframework.data.geo.Polygon;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Optional;

@RestController
@RequestMapping("/rest/area")
@RequiredArgsConstructor
public class AreaController {
    private final AreaService areaService;

    @GetMapping("/whereami")
    public ResponseEntity<?> getAreaForCoordinates(@RequestBody CoordinateDto coordinateDto){
        Optional<Area> currentArea = areaService.getCurrentArea(coordinateDto.latitude(), coordinateDto.longitude());
        if(currentArea.isEmpty()){
            return HttpUtils.produceNotFoundResponse("No area for the given coordinates: " + coordinateDto);
        }

        return ResponseEntity.ok(new AreaDto(currentArea.get()));
    }

    @GetMapping
    public ResponseEntity<?> getAllAreas(){
        return ResponseEntity.ok(areaService.findAll().stream().map(AreaDto::new).toList());
    }

    @PostMapping
    public ResponseEntity<?> createArea(@RequestBody AreaDto areaDto){
        Area area = Area.builder()
                .areaPolygon(new Polygon(areaDto.coordinates().stream()
                        .map(x -> new Point(x.latitude(), x.longitude())).toList()))
                .zone(areaDto.zone())
                .name(areaDto.name()).build();
        return ResponseEntity.ok(areaService.save(area));
    }
}
