package com.frozenheart.backend.modules.report.repository;

import com.frozenheart.backend.core.entity.socialinteraction.Report;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ReportRepository extends JpaRepository<Report, Long> {
}
