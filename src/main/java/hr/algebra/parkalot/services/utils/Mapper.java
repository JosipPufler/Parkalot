package hr.algebra.parkalot.services.utils;

import hr.algebra.parkalot.configs.PasswordEncoderProvider;
import hr.algebra.parkalot.models.Role;
import hr.algebra.parkalot.models.dto.AppealCreationDto;
import hr.algebra.parkalot.models.dto.PermitRequestDto;
import hr.algebra.parkalot.models.dto.RegistrationForm;
import hr.algebra.parkalot.models.dto.VehicleDto;
import hr.algebra.parkalot.models.entity.*;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class Mapper {
    public static Permit mapPermitRequestToPermit(PermitRequestDto permitRequestDto, Vehicle vehicle, Area area){
        return Permit.builder()
                .area(area)
                .permitDuration(permitRequestDto.permitDuration())
                .price(area.getZone().getPrice() * permitRequestDto.permitDuration().getMultiplier())
                .vehicle(vehicle)
                .startDate(permitRequestDto.startDate())
                .build();
    }

    public static Appeal mapAppealRequestToAppeal(AppealCreationDto appealCreationDto, Vehicle vehicle, Violation violation){
        return Appeal.builder()
                .title(appealCreationDto.title())
                .createdAt(LocalDateTime.now())
                .deadline(LocalDate.now().plusDays(7))
                .description(appealCreationDto.message())
                .violation(violation)
                .user(vehicle.getOwner())
                .resolved(Boolean.FALSE)
                .rejected(Boolean.FALSE)
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

    public static Vehicle mapVehicleDtoToVehicle(VehicleDto vehicleDto, User user){
        return Vehicle.builder()
                .make(vehicleDto.make())
                .model(vehicleDto.model())
                .year(vehicleDto.year())
                .licencePlate(vehicleDto.licencePlate())
                .owner(user)
                .build();
    }
}
