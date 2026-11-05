package hr.algebra.parkalot.configs;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.PrecisionModel;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class GeometryFactoryProvider {
    public static final GeometryFactory geometryFactory = new GeometryFactory(new PrecisionModel(), 4326);
}
