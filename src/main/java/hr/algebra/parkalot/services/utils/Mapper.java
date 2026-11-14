package hr.algebra.parkalot.services.utils;

import hr.algebra.parkalot.configs.GeometryFactoryProvider;
import hr.algebra.parkalot.configs.PasswordEncoderProvider;
import hr.algebra.parkalot.models.enums.Role;
import hr.algebra.parkalot.models.enums.Zone;
import hr.algebra.parkalot.models.dtos.*;
import hr.algebra.parkalot.models.entities.*;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.Polygon;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class Mapper {
    public static Permit mapPermitRequestToPermit(PermitRequestDto permitRequestDto, Vehicle vehicle, Area area, User user){
        return Permit.builder()
                .area(area)
                .permitDuration(permitRequestDto.permitDuration())
                .vehicle(vehicle)
                .price(area.getZone().getPrice() * permitRequestDto.permitDuration().getMultiplier())
                .createdAt(LocalDateTime.now())
                .buyer(user)
                .startDate(permitRequestDto.startDate())
                .build();
    }

    public static Appeal mapAppealRequestToAppeal(AppealCreationDto appealCreationDto, Violation violation, User user){
        return Appeal.builder()
                .title(appealCreationDto.title())
                .createdAt(LocalDateTime.now())
                .deadline(LocalDate.now().plusDays(7))
                .description(appealCreationDto.message())
                .violation(violation)
                .user(user)
                .resolved(Boolean.FALSE)
                .rejected(Boolean.FALSE)
                .build();
    }

    public static Appeal mapAppealRequestToAppeal(AdminAppealCreationDto appealCreationDto, Violation violation, User user){
        return mapAppealRequestToAppeal(new AppealCreationDto(appealCreationDto), violation, user);
    }

    public static Household mapHouseholdDtoToEntity(HouseholdDto householdDto) {
        return Household.builder()
                .street(householdDto.street())
                .houseNumber(householdDto.houseNumber())
                .city(householdDto.city())
                .postalCode(householdDto.postalCode())
                .country(householdDto.country())
                .build();
    }

    public static User mapRegistrationFormToUser(RegistrationForm registrationForm){
        return User.builder()
                .email(registrationForm.email())
                .password(PasswordEncoderProvider.getStaticPasswordEncoder().encode(registrationForm.password()))
                .firstName(registrationForm.firstName())
                .lastName(registrationForm.lastName())
                .role(Role.USER)
                .build();
    }

    public static Vehicle mapVehicleDtoToVehicle(VehicleDto vehicleDto){
        return Vehicle.builder()
                .make(vehicleDto.make())
                .model(vehicleDto.model())
                .year(vehicleDto.year())
                .licencePlate(vehicleDto.licencePlate())
                .emissionClass(vehicleDto.emissionClass())
                .build();
    }

    public static Area mapAreaDtoToArea(AreaDto areaDto) {
        List<Coordinate> coordinates = new ArrayList<>(areaDto.coordinates().stream()
                .map(c -> new Coordinate(c.longitude(), c.latitude()))
                .toList());

        coordinates.add(coordinates.getFirst());

        Polygon polygon = GeometryFactoryProvider.geometryFactory.createPolygon(coordinates.toArray(new Coordinate[0]));

        return Area.builder()
                .id(areaDto.id())
                .areaPolygon(polygon)
                .zone(areaDto.zone())
                .name(areaDto.name())
                .build();
    }

    public static PermitEligibilityRule mapPermitEligibilityRuleFromDto(EligibilityRuleDto eligibilityRuleDto) {
        return PermitEligibilityRule.builder()
                .requiresResidentAddress(eligibilityRuleDto.requiresResidentAddress())
                .maxVehiclesPerHousehold(eligibilityRuleDto.maxVehiclesPerHousehold())
                .minimumEmissionClass(eligibilityRuleDto.emissionClass())
                .build();
    }
}
