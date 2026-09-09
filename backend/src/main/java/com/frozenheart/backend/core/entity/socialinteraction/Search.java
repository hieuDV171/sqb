package com.frozenheart.backend.core.entity.socialinteraction;

import com.frozenheart.backend.core.entity.user.User;
import jakarta.persistence.*;
import lombok.*;
import java.time.Instant;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name = "searches", uniqueConstraints = {
                @UniqueConstraint(name = "uk_user_query_text", columnNames = { "user_id", "query_text" })
})
public class Search {
        @Id
        @GeneratedValue(strategy = GenerationType.SEQUENCE)
        private Long id;

        private String queryText;

        private Instant lastSearchedAt;

        @Column(updatable = false)
        private Instant createdAt;

        // --------------------------

        @ManyToOne(fetch = FetchType.LAZY)
        @JoinColumn(name = "user_id")
        private User user;

        // --------------------------
}
