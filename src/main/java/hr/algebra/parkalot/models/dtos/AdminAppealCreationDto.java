package hr.algebra.parkalot.models.dtos;

import org.springframework.web.multipart.MultipartFile;

import java.util.List;

public record AdminAppealCreationDto (Long violationId, List<MultipartFile> evidence, String title, String message, Long ownerId) {
}
