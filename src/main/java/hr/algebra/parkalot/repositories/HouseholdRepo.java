package hr.algebra.parkalot.repositories;

import hr.algebra.parkalot.models.entities.Household;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface HouseholdRepo extends JpaRepository<Household, Long> {
    Optional<Household> findByStreetAndPostalCodeAndCityAndCountryAndHouseNumber(String street, String postalCode, String city, String country, String houseNumber);
}
