package hr.algebra.parkalot.services;

import hr.algebra.parkalot.exceptions.LocalizedBadRequestException;
import hr.algebra.parkalot.models.entities.*;
import hr.algebra.parkalot.models.enums.LocalizedExceptionKeys;
import hr.algebra.parkalot.repositories.AreaRepo;
import hr.algebra.parkalot.repositories.PermitRepo;
import lombok.RequiredArgsConstructor;
import org.jspecify.annotations.NonNull;
import org.springframework.stereotype.Service;

import java.time.LocalDate;

@Service
@RequiredArgsConstructor
public class PermitEligibilityService {

    private final PermitRepo permitRepo;
    private final AreaRepo areaRepo;

    public void validateEligibility(
            User user,
            Vehicle vehicle,
            Area area
    ) {
        PermitEligibilityRule rule = getRule(vehicle, area);

        Household household = user.getHousehold();

        if (household == null) {
            throw new LocalizedBadRequestException(LocalizedExceptionKeys.USER_NO_HOUSEHOLD);
        }

        if (rule.getRequiresResidentAddress()
                && !isHouseholdAddressInsideArea(household, area)) {
            throw new LocalizedBadRequestException(LocalizedExceptionKeys.HOUSEHOLD_ADDRESS_NOT_ELIGIBLE);
        }

        long existingPermits = permitRepo
                .findStartedPaidPermitsByHouseholdAndArea(
                        household.getId(), area.getId(), LocalDate.now())
                .stream()
                .filter(permit -> !permit.isExpired())
                .map(permit -> permit.getVehicle().getId())
                .distinct()
                .count();

        if (existingPermits >= rule.getMaxVehiclesPerHousehold()) {
            throw new LocalizedBadRequestException(LocalizedExceptionKeys.HOUSEHOLD_VEHICLE_LIMIT);
        }
    }

    private static @NonNull PermitEligibilityRule getRule(Vehicle vehicle, Area area) {
        PermitEligibilityRule rule = area.getEligibilityRule();

        if (rule == null) {
            throw new LocalizedBadRequestException(LocalizedExceptionKeys.AREA_NO_ELIGIBILITY_RULE);
        }

        if (!vehicle.getEmissionClass()
                .meetsRequirement(rule.getMinimumEmissionClass())) {

            throw new LocalizedBadRequestException(LocalizedExceptionKeys.VEHICLE_EMISSION);
        }
        return rule;
    }

    private boolean isHouseholdAddressInsideArea(
            Household household,
            Area area
    ) {
        return areaRepo.isHouseholdInArea(household.getId(), area.getId());
    }
}
