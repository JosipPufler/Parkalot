package hr.algebra.parkalot.models.dto;

import org.springframework.web.multipart.MultipartFile;

public record PermitCheckImageRequestDto(MultipartFile multipartFile, CoordinateDto coordinates) {
}
