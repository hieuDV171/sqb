package com.frozenheart.backend.core.entity.session;

import jakarta.persistence.*;
import lombok.*;

import java.util.Set;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name = "topics")
public class Topic {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE)
    private Long id;

    @Column(length = 100, nullable = false)
    private String name;

    @Column(length = 10, nullable = false, unique = true)
    String code;
    // -------------------
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "subject_id")
    private Subject subject;
    // -------------------

    @Singular
    @OneToMany(mappedBy = "topic")
    private Set<Question> questions;
}
