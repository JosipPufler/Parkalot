package hr.algebra.parkalot.repositories;

import hr.algebra.parkalot.models.entities.Area;
import org.locationtech.jts.geom.Polygon;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface AreaRepo extends JpaRepository<Area, Long> {
    @Query(value = """
        SELECT * FROM areas g 
        WHERE ST_Contains(g.area_polygon, ST_SetSRID(ST_MakePoint(:lng, :lat), 4326))
        """, nativeQuery = true)
    List<Area> findAreasByCoordinates(@Param("lat") double lat, @Param("lng") double lng);
    @Query(value = """
        SELECT ST_Contains(
            a.area_polygon,
            h.location
        )
        FROM areas a
        JOIN households h ON h.id = :householdId
        WHERE a.id = :areaId;
    """, nativeQuery = true)
    Boolean isHouseholdInArea(Long householdId, Long areaId);
    @Query(value = """
    SELECT EXISTS (
        SELECT 1
        FROM areas a
        WHERE ST_Intersects(a.area_polygon, :polygon)
          AND ST_Area(ST_Intersection(a.area_polygon, :polygon)) > 0
          AND (:areaId IS NULL OR a.id <> :areaId)
    )
    """, nativeQuery = true)
    boolean overlapsExistingArea(
            @Param("polygon") Polygon polygon,
            @Param("areaId") Long areaId
    );
}
