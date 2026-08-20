package com.frozenheart.backend.modules.session.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.frozenheart.backend.core.entity.session.Session;

@Repository
public interface SessionRepository extends JpaRepository<Session, Long> {

}
