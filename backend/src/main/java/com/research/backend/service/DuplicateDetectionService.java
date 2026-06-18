package com.research.backend.service;

import com.research.backend.dto.DuplicateCheckResult;
import com.research.backend.dto.SausageDetectionDto;
import com.research.backend.entity.DuplicateCheckReport;

import java.util.List;

public interface DuplicateDetectionService {

    DuplicateCheckResult checkPaper(Long paperId);

    List<DuplicateCheckResult> batchCheck(List<Long> paperIds);

    List<DuplicateCheckReport> getCheckHistory(Long paperId);

    DuplicateCheckReport updateReportStatus(Long reportId, String status, String note);

    SausageDetectionDto detectSausagePapers(Long paperId);
}
