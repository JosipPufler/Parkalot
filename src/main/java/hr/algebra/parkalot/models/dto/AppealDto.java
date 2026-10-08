package hr.algebra.parkalot.models.dto;

import org.springframework.web.multipart.MultipartFile;

public record AppealDto(Long violationId, MultipartFile evidence, String title, String message) {
}
