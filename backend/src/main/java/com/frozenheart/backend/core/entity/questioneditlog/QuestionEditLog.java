package com.frozenheart.backend.core.entity.questioneditlog;

import com.frozenheart.backend.core.entity.user.User;
import com.frozenheart.backend.core.entity.session.Session;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "question_edit_logs")
public class QuestionEditLog {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE)
    private Long id;

    @Column(columnDefinition = "TEXT")
    private String aiPrompt;

    @Column(columnDefinition = "TEXT")
    private String aiResponse;

    @Enumerated(value = EnumType.STRING)
    @Column(length = 20)
    private AiLogType logType;

    @Enumerated(value = EnumType.STRING)
    @Column(length = 20)
    private AiLogStatus status;

    // ------------------------
    // CẦN HIỆU CHỈNH
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "JSONB")
    private AiMetatdata aiMetatdata;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "JSONB")
    private AiLogContext context;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "JSONB")
    private HallucinationAudit hallucinationAudit;
    // ----------------------------

    // ---------------------
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "lecturer_id")
    private User lecturer;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "session_id")
    private Session session;
    // ---------------------



}
