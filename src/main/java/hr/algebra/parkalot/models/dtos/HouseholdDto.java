package hr.algebra.parkalot.models.dtos;

import hr.algebra.parkalot.models.entities.Household;

public record HouseholdDto(Long id, String street, String houseNumber, String city, String postalCode, String country) {
    public HouseholdDto(Household household) {
        this(household.getId(), household.getStreet(), household.getHouseNumber(), household.getCity(), household.getPostalCode(), household.getCountry());
    }
}
