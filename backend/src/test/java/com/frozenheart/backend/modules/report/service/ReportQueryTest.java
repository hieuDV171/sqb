package com.frozenheart.backend.modules.report.service;

import com.frozenheart.backend.core.entity.post.Post;
import com.frozenheart.backend.core.entity.socialinteraction.Comment;
import com.frozenheart.backend.core.entity.socialinteraction.ModerationAction;
import com.frozenheart.backend.core.entity.user.User;
import com.frozenheart.backend.modules.activityfeed.repository.ActivityFeedRepository;
import com.frozenheart.backend.modules.post.repository.PostRepository;
import com.frozenheart.backend.modules.report.repository.ModerationActionRepository;
import com.frozenheart.backend.modules.report.repository.ReportRepository;
import com.frozenheart.backend.modules.socialinteraction.repository.CommentRepository;
import org.hibernate.Hibernate;
import org.hibernate.boot.MetadataSources;
import org.hibernate.boot.registry.StandardServiceRegistryBuilder;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.DefaultResourceLoader;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.orm.jpa.persistenceunit.PersistenceManagedTypesScanner;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.assertj.core.api.Assertions.assertThat;

class ReportQueryTest {
    @Test
    void hibernateParsesModerationAndAffectedReadQueries() {
        var registry = new StandardServiceRegistryBuilder()
                .applySetting("hibernate.dialect", "org.hibernate.dialect.PostgreSQLDialect")
                .applySetting("hibernate.boot.allow_jdbc_metadata_access", "false")
                .build();
        try {
            var metadata = new MetadataSources(registry);
            new PersistenceManagedTypesScanner(new DefaultResourceLoader())
                    .scan("com.frozenheart.backend.core.entity")
                    .getManagedClassNames().forEach(metadata::addAnnotatedClassName);
            var mappings = metadata.buildMetadata();
            String actionTable = mappings.getEntityBinding(ModerationAction.class.getName()).getTable().getName();
            for (var target : List.of(Post.class, Comment.class, User.class)) {
                var table = mappings.getEntityBinding(target.getName()).getTable();
                assertThat(table.getForeignKeyCollection()).anySatisfy(foreignKey -> {
                    assertThat(foreignKey.getReferencedTable().getName()).isEqualTo(actionTable);
                    assertThat(foreignKey.getColumn(0).getName()).isEqualTo("moderation_action_id");
                    assertThat(foreignKey.getColumn(0).isNullable()).isTrue();
                });
            }
            try (var factory = mappings.buildSessionFactory();
                 var session = factory.openSession()) {
                var action = session.getReference(ModerationAction.class, 90L);
                var view = ReportTargets.postView(Post.builder().activeModerationAction(action).build());
                assertThat(view.actionId()).isEqualTo(90L);
                assertThat(Hibernate.isInitialized(action)).isFalse();
                for (var repository : List.of(ReportRepository.class, ModerationActionRepository.class,
                        PostRepository.class, CommentRepository.class, ActivityFeedRepository.class)) {
                    for (var method : repository.getDeclaredMethods()) {
                        Query query = method.getAnnotation(Query.class);
                        if (query == null || query.nativeQuery()) continue;
                        if (method.isAnnotationPresent(Modifying.class)) {
                            assertDoesNotThrow(() -> session.createMutationQuery(query.value()), method::toGenericString);
                        } else {
                            assertDoesNotThrow(() -> session.createQuery(query.value(), Object.class), method::toGenericString);
                        }
                    }
                }
            }
        } finally {
            StandardServiceRegistryBuilder.destroy(registry);
        }
    }
}
