package com.frozenheart.backend.modules.session.component;

import com.frozenheart.backend.core.entity.session.Semester;
import com.frozenheart.backend.modules.session.repository.SemesterRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import java.util.concurrent.atomic.AtomicReference;

@Slf4j
@Component
@RequiredArgsConstructor
public class CurrentSemesterHolder {

    private final SemesterRepository semesterRepository;
    private final AtomicReference<Semester> cachedSemester = new AtomicReference<>();

    @EventListener(ApplicationReadyEvent.class)
    public void init() {
        refreshCache();
    }

    public Semester getCurrentSemester() {
        Semester s = cachedSemester.get();
        if (s == null) {
            s = semesterRepository.findByActiveTrue().orElse(null);
            if (s != null) {
                cachedSemester.set(s);
            }
        }
        return s;
    }

    public void setCurrentSemester(Semester semester) {
        cachedSemester.set(semester);
        log.info("[CurrentSemesterHolder] Updated active semester cache to id={}, name={}",
                semester != null ? semester.getId() : "null",
                semester != null ? semester.getName() : "null");
    }

    public void clearCache() {
        cachedSemester.set(null);
        log.info("[CurrentSemesterHolder] Cleared active semester cache");
    }

    public void refreshCache() {
        Semester activeSemester = semesterRepository.findByActiveTrue().orElse(null);
        cachedSemester.set(activeSemester);
        log.info("[CurrentSemesterHolder] Initialized active semester cache: {}",
                activeSemester != null ? activeSemester.getName() : "None");
    }
}
