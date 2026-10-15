package hr.algebra.parkalot.repositories;

import hr.algebra.parkalot.models.entity.Area;
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
}
