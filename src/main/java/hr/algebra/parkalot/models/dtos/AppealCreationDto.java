package hr.algebra.parkalot.models.dtos;

import org.springframework.web.multipart.MultipartFile;

import java.util.List;

public record AppealCreationDto(Long violationId, List<MultipartFile> evidence, String title, String message) {
    public AppealCreationDto(AdminAppealCreationDto adminAppealCreationDto) {
        this(adminAppealCreationDto.violationId(), adminAppealCreationDto.evidence(), adminAppealCreationDto.title(), adminAppealCreationDto.title());
    }
}
