package hr.algebra.parkalot.controllers.rest;

import hr.algebra.parkalot.models.Zone;
import hr.algebra.parkalot.models.entity.Area;
import hr.algebra.parkalot.models.dto.AreaDto;
import hr.algebra.parkalot.models.dto.CoordinateDto;
import hr.algebra.parkalot.services.db.AreaService;
import hr.algebra.parkalot.services.utils.HttpUtils;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.Polygon;
import org.locationtech.jts.io.ParseException;
import org.locationtech.jts.io.WKBWriter;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.List;
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

        try {
            return ResponseEntity.ok(new AreaDto(currentArea.get()));
        } catch (ParseException e) {
            return HttpUtils.produceInternalErrorResponse(e.getMessage());
        }
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('USER', 'OFFICER', 'ADMIN')")
    public ResponseEntity<?> getAllAreas() throws ParseException {
        List<AreaDto> list = new ArrayList<>();
        for (Area area : areaService.findAll()) {
            AreaDto areaDto = new AreaDto(area);
            list.add(areaDto);
        }
        return ResponseEntity.ok(list);
    }

    @PostMapping()
    @PreAuthorize("hasAnyRole('OFFICER', 'ADMIN')")
    public ResponseEntity<?> createArea(@Valid @RequestBody AreaDto areaDto){
        List<Coordinate> coordinates = new ArrayList<>(areaDto.coordinates().stream()
                .map(c -> new Coordinate(c.longitude(), c.latitude()))
                .toList());

        coordinates.add(coordinates.getFirst());

        Polygon polygon = new GeometryFactory().createPolygon(coordinates.toArray(new Coordinate[0]));
        byte[] polygonWkb = new WKBWriter().write(polygon);

        Area area = Area.builder()
                .areaPolygon(polygonWkb)
                .zone(Zone.valueOf(areaDto.zone()))
                .name(areaDto.name())
                .build();
        return ResponseEntity.ok(areaService.save(area));
    }
}
