package hr.algebra.parkalot.helper;

import hr.algebra.parkalot.configs.GeometryFactoryProvider;
import hr.algebra.parkalot.models.CustomUserDetails;
import hr.algebra.parkalot.models.entities.*;
import hr.algebra.parkalot.models.enums.EmissionClass;
import hr.algebra.parkalot.models.enums.PermitDuration;
import hr.algebra.parkalot.models.enums.Role;
import hr.algebra.parkalot.models.enums.Zone;
import hr.algebra.parkalot.repositories.*;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.Point;
import org.locationtech.jts.geom.Polygon;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors;
import org.springframework.stereotype.Component;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;

/**
 * Builds and persists test fixtures through the real repositories. Meant to be used inside the
 * (rolled back) transaction of an integration test.
 */
@Component
public class TestData {
    public static final String PASSWORD = "pass";

    @Autowired private UserRepo userRepo;
    @Autowired private HouseholdRepo householdRepo;
    @Autowired private VehicleRepo vehicleRepo;
    @Autowired private AreaRepo areaRepo;
    @Autowired private PermitRepo permitRepo;
    @Autowired private ParkingCheckLogRepo parkingCheckLogRepo;
    @Autowired private ViolationRepo violationRepo;
    @Autowired private AppealRepo appealRepo;
    @Autowired private AppealEvidenceRepo appealEvidenceRepo;
    @Autowired private PermitArchiveRepo permitArchiveRepo;
    @Autowired private PasswordEncoder passwordEncoder;
    @PersistenceContext private EntityManager em;

    public User user(String email, Role role) {
        return userRepo.saveAndFlush(User.builder()
                .firstName("First")
                .lastName("Last")
                .email(email)
                .password(passwordEncoder.encode(PASSWORD))
                .role(role)
                .build());
    }

    public User userInHousehold(String email, Role role, Household household) {
        User user = user(email, role);
        user.setHousehold(household);
        return userRepo.saveAndFlush(user);
    }

    /** A household located at the given longitude/latitude. */
    public Household household(String street, double lon, double lat) {
        Point location = GeometryFactoryProvider.geometryFactory.createPoint(new Coordinate(lon, lat));
        return householdRepo.saveAndFlush(Household.builder()
                .street(street)
                .houseNumber("1")
                .city("Zagreb")
                .postalCode("10000")
                .country("Croatia")
                .location(location)
                .build());
    }

    /** First owner is the primary owner. */
    public Vehicle vehicle(String plate, EmissionClass emissionClass, User... owners) {
        Vehicle vehicle = Vehicle.builder()
                .make("Volkswagen")
                .model("Golf")
                .year(2020)
                .licencePlate(plate)
                .emissionClass(emissionClass)
                .build();
        for (int i = 0; i < owners.length; i++) {
            VehicleOwnership ownership = VehicleOwnership.builder()
                    .user(owners[i])
                    .vehicle(vehicle)
                    .ownershipStartDate(LocalDate.now())
                    .primaryOwner(i == 0)
                    .build();
            vehicle.getOwnerships().add(ownership);
            owners[i].getVehicleOwnerships().add(ownership);
        }
        return vehicleRepo.saveAndFlush(vehicle);
    }

    public Polygon square(double minLon, double minLat, double size) {
        Coordinate[] ring = {
                new Coordinate(minLon, minLat),
                new Coordinate(minLon, minLat + size),
                new Coordinate(minLon + size, minLat + size),
                new Coordinate(minLon + size, minLat),
                new Coordinate(minLon, minLat)
        };
        return GeometryFactoryProvider.geometryFactory.createPolygon(ring);
    }

    /** Area covering [minLon, minLon+size] x [minLat, minLat+size] with an eligibility rule. */
    public Area area(String name, Zone zone, double minLon, double minLat, double size,
                     boolean requiresResident, int maxVehicles, EmissionClass minEmission) {
        Area area = Area.builder()
                .name(name)
                .zone(zone)
                .areaPolygon(square(minLon, minLat, size))
                .build();
        area.setEligibilityRule(PermitEligibilityRule.builder()
                .requiresResidentAddress(requiresResident)
                .maxVehiclesPerHousehold(maxVehicles)
                .minimumEmissionClass(minEmission)
                .build());
        return areaRepo.saveAndFlush(area);
    }

    public Area saveArea(Area area) {
        return areaRepo.saveAndFlush(area);
    }

    public Area area(String name, double minLon, double minLat, double size) {
        return area(name, Zone.ZONE_1, minLon, minLat, size, false, 5, EmissionClass.EURO_1);
    }

    public Permit permit(Vehicle vehicle, Area area, User buyer, LocalDate start, PermitDuration duration, boolean paid) {
        Permit permit = Permit.builder()
                .vehicle(vehicle)
                .area(area)
                .buyer(buyer)
                .startDate(start)
                .permitDuration(duration)
                .price(area.getZone().getPrice() * duration.getMultiplier())
                .createdAt(LocalDateTime.now())
                .paymentTime(paid ? LocalDateTime.now() : null)
                .paypalId("ORDER-" + System.nanoTime())
                .build();
        permit = permitRepo.saveAndFlush(permit);
        vehicle.getPermits().add(permit);
        return permit;
    }

    public ParkingCheckLog parkingLog(User officer, String plate, Area area, LocalDateTime timestamp) {
        return parkingCheckLogRepo.saveAndFlush(ParkingCheckLog.builder()
                .officer(officer)
                .licencePlate(plate)
                .resolvedArea(area)
                .latitude(0.5)
                .longitude(0.5)
                .inViolation(true)
                .timestamp(timestamp)
                .build());
    }

    public Violation violation(User officer, String plate, Area area, boolean active) {
        ParkingCheckLog log = parkingLog(officer, plate, area, LocalDateTime.now());
        Violation violation = violationRepo.saveAndFlush(Violation.builder()
                .active(active)
                .message("msg")
                .price(8.99)
                .timestamp(LocalDateTime.now())
                .parkingCheckLog(log)
                .appeals(new ArrayList<>())
                .build());
        log.setViolation(violation);
        parkingCheckLogRepo.saveAndFlush(log);
        return violation;
    }

    public Appeal appeal(User user, Violation violation) {
        return appealRepo.saveAndFlush(Appeal.builder()
                .user(user)
                .violation(violation)
                .title("Unfair")
                .description("Not my fault")
                .createdAt(LocalDateTime.now())
                .deadline(LocalDate.now().plusDays(7))
                .resolved(false)
                .rejected(false)
                .build());
    }

    public AppealEvidence evidence(Appeal appeal, String filename, String mediaType, byte[] bytes) {
        return appealEvidenceRepo.saveAndFlush(AppealEvidence.builder()
                .appeal(appeal)
                .filename(filename)
                .mediaType(mediaType)
                .evidence(bytes)
                .build());
    }

    public PermitArchive archive(Permit permit) {
        return permitArchiveRepo.saveAndFlush(new PermitArchive(permit));
    }

    public void flushAndClear() {
        em.flush();
        em.clear();
    }

    /** Authenticates the request as the given user, the same way TokenFilter would. */
    public static RequestPostProcessor auth(User user) {
        CustomUserDetails principal = new CustomUserDetails(user);
        return SecurityMockMvcRequestPostProcessors.authentication(
                new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities()));
    }
}
