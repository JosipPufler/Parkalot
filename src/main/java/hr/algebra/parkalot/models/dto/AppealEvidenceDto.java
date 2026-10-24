package hr.algebra.parkalot.models.dto;

import hr.algebra.parkalot.models.entity.AppealEvidence;

public record AppealEvidenceDto(Long id, String filename, String mediaType) {
    public AppealEvidenceDto(AppealEvidence appealEvidence) {
        this(appealEvidence.getId(), appealEvidence.getFilename(), appealEvidence.getMediaType());
    }
}
