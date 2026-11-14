package hr.algebra.parkalot.models.dtos;

import hr.algebra.parkalot.models.entities.PermitEligibilityRule;
import hr.algebra.parkalot.models.enums.EmissionClass;

public record EligibilityRuleDto (Boolean requiresResidentAddress, Integer maxVehiclesPerHousehold, EmissionClass emissionClass) {
    public EligibilityRuleDto(PermitEligibilityRule eligibilityRule){
        this(eligibilityRule.getRequiresResidentAddress(), eligibilityRule.getMaxVehiclesPerHousehold(), eligibilityRule.getMinimumEmissionClass());
    }
}
