package hr.algebra.parkalot.models.dto;

import java.time.LocalDateTime;

public record PermitCheckResultDto(PermitCheckRequestDto requestDto, LocalDateTime requestTime, Boolean valid,
                                   PermitDto permitDto, ViolationDto violationDto) {
}
