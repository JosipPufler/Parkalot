package hr.algebra.parkalot.models.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalDateTime;

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

    @ManyToOne(optional = false)
    private User user;
    @ManyToOne(optional = false)
    private Violation violation;
}
