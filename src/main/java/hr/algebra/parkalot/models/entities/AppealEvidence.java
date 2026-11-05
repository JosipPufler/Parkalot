package hr.algebra.parkalot.models.entities;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "appeal_evidence")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AppealEvidence {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "evidence", columnDefinition = "BYTEA") // use "BLOB" for H2 if needed
    private byte[] evidence;
    @NotBlank
    private String mediaType;
    @NotBlank
    private String filename;

    @ManyToOne(optional = false)
    @JoinColumn(name = "appeal_id", nullable = false)
    private Appeal appeal;
}
