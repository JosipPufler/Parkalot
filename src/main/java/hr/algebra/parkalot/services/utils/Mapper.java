package hr.algebra.parkalot.services.utils;

import hr.algebra.parkalot.configs.PasswordEncoderProvider;
import hr.algebra.parkalot.models.Role;
import hr.algebra.parkalot.models.dto.AppealDto;
import hr.algebra.parkalot.models.dto.PermitRequestDto;
import hr.algebra.parkalot.models.dto.RegistrationForm;
import hr.algebra.parkalot.models.entity.*;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class Mapper {
    public static Permit mapPermitRequestToPermit(PermitRequestDto permitRequestDto, Vehicle vehicle){
        return Permit.builder()
                .zone(permitRequestDto.zone())
                .permitDuration(permitRequestDto.permitDuration())
                .price(permitRequestDto.zone().getPrice() * permitRequestDto.permitDuration().getMultiplier())
                .vehicle(vehicle)
                .startDate(permitRequestDto.startDate())
                .build();
    }

    public static Appeal mapAppealRequestToAppeal(AppealDto appealDto, Vehicle vehicle, Violation violation){
        return Appeal.builder()
                .title(appealDto.title())
                .createdAt(LocalDateTime.now())
                .deadline(LocalDate.now().plusDays(7))
                .description(appealDto.message())
                .violation(violation)
                .user(vehicle.getOwner())
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
}
