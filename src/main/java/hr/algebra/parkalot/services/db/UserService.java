package hr.algebra.parkalot.services.db;

import hr.algebra.parkalot.exceptions.LocalizedConflictException;
import hr.algebra.parkalot.exceptions.LocalizedNotFoundException;
import hr.algebra.parkalot.models.dtos.RegistrationForm;
import hr.algebra.parkalot.models.dtos.UserDto;
import hr.algebra.parkalot.models.entities.Household;
import hr.algebra.parkalot.models.entities.User;
import hr.algebra.parkalot.models.enums.LocalizedExceptionKeys;
import hr.algebra.parkalot.repositories.HouseholdRepo;
import hr.algebra.parkalot.repositories.UserRepo;
import hr.algebra.parkalot.services.GeocodingService;
import hr.algebra.parkalot.services.utils.Mapper;
import org.springframework.stereotype.Service;

import java.util.Objects;
import java.util.Optional;

@Service
public class UserService extends GeneralCrudService<User, UserRepo> {
    private final HouseholdRepo householdRepo;
    private final GeocodingService geocodingService;

    protected UserService(UserRepo repository, HouseholdRepo householdRepo, GeocodingService geocodingService) {
        super(repository);
        this.householdRepo = householdRepo;
        this.geocodingService = geocodingService;
    }

    public Boolean existsByEmail(String email) {
        return repository.existsByEmail(email);
    }

    public Optional<User> findByEmail(String email) {
        return repository.findByEmail(email);
    }

    public User registerUser(RegistrationForm registrationForm) {
        Household household = householdRepo.findByStreetAndPostalCodeAndCityAndCountryAndHouseNumber(
                registrationForm.household().street(), registrationForm.household().postalCode(),
                registrationForm.household().city(), registrationForm.household().country(),
                registrationForm.household().houseNumber()).orElseGet(() -> {
            Household householdLocal = Mapper.mapHouseholdDtoToEntity(registrationForm.household());
            householdLocal.setLocation(geocodingService.geocode(householdLocal));
            return householdLocal;
        });

        Household savedHousehold = householdRepo.save(household);
        User user = Mapper.mapRegistrationFormToUser(registrationForm);
        user.setHousehold(savedHousehold);
        return repository.save(user);
    }

    public User updateUser(UserDto userDto) {
        User user = findById(userDto.id()).orElseThrow(() ->
                new LocalizedNotFoundException(
                        LocalizedExceptionKeys.USER_NOT_FOUND, userDto.id()));
        Household household = householdRepo.findById(userDto.household().id()).orElseThrow(() ->
                new LocalizedNotFoundException(
                        LocalizedExceptionKeys.HOUSEHOLD_NOT_FOUND, userDto.household().id()));

        if (!Objects.equals(user.getEmail(), userDto.email()) && existsByEmail(userDto.email())) {
            throw new LocalizedConflictException(LocalizedExceptionKeys.USER_EMAIL_CONFLICT, userDto.email(), userDto.id());
        }

        if (household.differs(userDto.household())) {
            Optional<Household> otherHousehold = householdRepo.findByStreetAndPostalCodeAndCityAndCountryAndHouseNumber(userDto.household().street(),
                    userDto.household().postalCode(), userDto.household().city(), userDto.household().country(), userDto.household().houseNumber());
            if (otherHousehold.isPresent())
                user.setHousehold(otherHousehold.get());
            else {
                Household newHousehold = Household.builder()
                        .street(userDto.household().street())
                        .houseNumber(userDto.household().houseNumber())
                        .city(userDto.household().city())
                        .country(userDto.household().country())
                        .postalCode(userDto.household().postalCode())
                        .location(geocodingService.geocode(household))
                        .build();
                newHousehold = householdRepo.save(newHousehold);
                user.setHousehold(newHousehold);
            }
        }

        user.setFirstName(userDto.firstName());
        user.setLastName(userDto.lastName());
        user.setEmail(userDto.email());
        user.setRole(userDto.role());
        return repository.save(user);
    }
}
