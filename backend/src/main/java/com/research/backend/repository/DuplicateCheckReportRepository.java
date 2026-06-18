package com.research.backend.repository;

import com.research.backend.entity.DuplicateCheckReport;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface DuplicateCheckReportRepository extends JpaRepository<DuplicateCheckReport, Long> {

    List<DuplicateCheckReport> findByPaperIdOrderByCheckTimeDesc(Long paperId);

    Optional<DuplicateCheckReport> findTopByPaperIdOrderByCheckTimeDesc(Long paperId);

    List<DuplicateCheckReport> findByReportStatusOrderByCheckTimeDesc(String reportStatus);
}
