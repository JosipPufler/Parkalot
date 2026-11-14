package hr.algebra.parkalot.models.enums;

import lombok.Getter;

public enum LocalizedExceptionKeys {
    PERMIT_NOT_FOUND("error.permit.notFound"),
    PERMIT_CONFLICTING("error.permit.conflicting"),
    VIOLATION_NOT_FOUND("error.violation.notFound"),
    EVIDENCE_NOT_FOUND("error.evidence.notFound"),
    VIOLATION_ALREADY_RESOLVED("error.violation.alreadyResolved"),
    VEHICLE_NOT_FOUND("error.vehicle.notFound"),
    VEHICLE_NOT_FOUND_PLATE("error.vehicle.notFound.plate"),
    USER_NOT_FOUND("error.user.notFound"),
    USER_EMAIL_CONFLICT("error.user.emailConflict"),
    USER_NO_HOUSEHOLD("error.user.noHousehold"),
    HOUSEHOLD_NOT_FOUND("error.household.notFound"),
    HOUSEHOLD_VEHICLE_LIMIT("error.household.vehicleLimitReached"),
    HOUSEHOLD_ADDRESS_NOT_ELIGIBLE("error.household.addressNotEligible"),
    AREA_NOT_FOUND("error.area.notFound"),
    AREA_OVERLAPPING("error.area.conflicting"),
    AREA_NO_ELIGIBILITY_RULE("error.area.noEligibilityRules"),
    APPEAL_NOT_FOUND("error.appeal.notFound"),
    APPEAL_ALREADY_RESOLVED("error.appeal.alreadyResolved"),
    VEHICLE_NO_OWNERS("error.vehicle.noOwner"),
    VEHICLE_NOT_IN_AREA("error.vehicle.notInArea"),
    VEHICLE_EMISSION("error.vehicle.emissionNotEligible"),
    VEHICLE_NOT_OWNED("error.vehicle.notOwned"),
    ADDRESS_NOT_LOCATED("error.address.notFound"),
    PAYMENT_CREATE_ORDER("error.payment.createOrder"),
    PAYMENT_ORDER_ID("error.payment.getOrderId"),
    PAYMENT_ACCESS_TOKEN("error.payment.getAccessToken"),
    PAYMENT_CAPTURE("error.payment.captureFailed"),
    PAYMENT_AMOUNT("error.payment.amountMismatch"),

    ;

    LocalizedExceptionKeys(String key) {
        this.key = key;
    }

    @Getter
    private final String key;
}
