package com.frozenheart.backend.core.entity.session;

import jakarta.persistence.*;
import lombok.*;
import java.time.Instant;
import java.time.LocalDate;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name = "semesters")
public class Semester {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE)
    private Long id;

    @Column(nullable = false, length = 100)
    private String name;

    private LocalDate startDate;

    @Builder.Default
    @Column(nullable = false)
    private boolean active = false;

    @Column(updatable = false)
    private Instant createdAt;

    @Builder.Default
    @Column(nullable = false)
    private boolean isFinalized = false;
}
