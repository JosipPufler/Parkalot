package hr.algebra.parkalot.models.entities;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "appeals")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Appeal {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank
    private String title;
    @NotBlank
    private String description;
    @NotNull
    private LocalDateTime createdAt;
    @NotNull
    private LocalDate deadline;
    @NotNull
    private Boolean resolved;
    @NotNull
    private Boolean rejected;

    @ManyToOne(optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;
    @ManyToOne(optional = false)
    @JoinColumn(name = "violation_id", nullable = false)
    private Violation violation;

    @OneToMany(mappedBy = "appeal", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<AppealEvidence> evidence = new ArrayList<>();
}
