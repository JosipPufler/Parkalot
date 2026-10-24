package hr.algebra.parkalot.models.dto;

import org.springframework.web.multipart.MultipartFile;

import java.util.List;

public record AppealCreationDto(Long violationId, List<MultipartFile> evidence, String title, String message) {
}
