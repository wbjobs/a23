package com.research.backend.controller;

import com.research.backend.common.Result;
import com.research.backend.dto.DuplicateCheckRequest;
import com.research.backend.dto.DuplicateCheckResult;
import com.research.backend.dto.SausageDetectionDto;
import com.research.backend.entity.DuplicateCheckReport;
import com.research.backend.entity.User;
import com.research.backend.exception.BusinessException;
import com.research.backend.neo4j.entity.Paper;
import com.research.backend.neo4j.repository.PaperRepository;
import com.research.backend.repository.DuplicateCheckReportRepository;
import com.research.backend.service.DuplicateDetectionService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/duplicate")
@RequiredArgsConstructor
public class DuplicateDetectionController {

    private final DuplicateDetectionService duplicateDetectionService;
    private final PaperRepository paperRepository;
    private final DuplicateCheckReportRepository duplicateCheckReportRepository;

    @PostMapping("/check")
    public Result<?> check(@RequestBody DuplicateCheckRequest request) {
        Long currentUserId = getCurrentUserId();
        String currentUserRole = getCurrentUserRole();

        if (request.getPaperId() != null) {
            checkPermission(request.getPaperId(), currentUserId, currentUserRole);
            DuplicateCheckResult result = duplicateDetectionService.checkPaper(request.getPaperId());
            return Result.success("检测完成", result);
        } else {
            if (!isAdmin(currentUserRole)) {
                return Result.fail(403, "只有管理员可以执行全量检测");
            }
            List<Paper> allPapers = paperRepository.findAll();
            List<Long> paperIds = allPapers.stream().map(Paper::getId).toList();
            List<DuplicateCheckResult> results = duplicateDetectionService.batchCheck(paperIds);
            return Result.success("批量检测完成", Map.of(
                    "totalCount", paperIds.size(),
                    "completedCount", results.size(),
                    "results", results
            ));
        }
    }

    @GetMapping("/paper/{paperId}/result")
    public Result<DuplicateCheckResult> getLatestResult(@PathVariable Long paperId) {
        Long currentUserId = getCurrentUserId();
        String currentUserRole = getCurrentUserRole();
        checkPermission(paperId, currentUserId, currentUserRole);

        DuplicateCheckReport latestReport = getLatestReport(paperId);
        if (latestReport == null) {
            return Result.success("暂无检测结果，请先执行检测", null);
        }

        DuplicateCheckResult result = DuplicateCheckResult.builder()
                .paperId(latestReport.getPaperId())
                .overallSimilarity(latestReport.getOverallSimilarity())
                .overallRiskLevel(latestReport.getOverallRiskLevel())
                .checkTime(latestReport.getCheckTime())
                .build();
        return Result.success(result);
    }

    @GetMapping("/paper/{paperId}/history")
    public Result<List<DuplicateCheckReport>> getCheckHistory(@PathVariable Long paperId) {
        Long currentUserId = getCurrentUserId();
        String currentUserRole = getCurrentUserRole();
        checkPermission(paperId, currentUserId, currentUserRole);

        List<DuplicateCheckReport> history = duplicateDetectionService.getCheckHistory(paperId);
        return Result.success(history);
    }

    @GetMapping("/report/{reportId}")
    public Result<DuplicateCheckReport> getReportDetail(@PathVariable Long reportId) {
        Long currentUserId = getCurrentUserId();
        String currentUserRole = getCurrentUserRole();

        DuplicateCheckReport report = duplicateCheckReportRepository.findById(reportId)
                .orElseThrow(() -> new BusinessException("报告不存在"));

        checkPermission(report.getPaperId(), currentUserId, currentUserRole);
        return Result.success(report);
    }

    @PutMapping("/report/{reportId}/status")
    public Result<DuplicateCheckReport> updateReportStatus(
            @PathVariable Long reportId,
            @RequestParam String status,
            @RequestParam(required = false) String note) {

        String currentUserRole = getCurrentUserRole();
        if (!isAdmin(currentUserRole)) {
            return Result.fail(403, "只有管理员可以复核检测报告");
        }

        DuplicateCheckReport report = duplicateDetectionService.updateReportStatus(reportId, status, note);
        return Result.success("复核完成", report);
    }

    @GetMapping("/paper/{paperId}/sausage")
    public Result<SausageDetectionDto> detectSausagePapers(@PathVariable Long paperId) {
        Long currentUserId = getCurrentUserId();
        String currentUserRole = getCurrentUserRole();
        checkPermission(paperId, currentUserId, currentUserRole);

        SausageDetectionDto result = duplicateDetectionService.detectSausagePapers(paperId);
        return Result.success(result);
    }

    private void checkPermission(Long paperId, Long currentUserId, String currentUserRole) {
        if (isAdmin(currentUserRole)) {
            return;
        }

        Paper paper = paperRepository.findById(paperId)
                .orElseThrow(() -> new BusinessException("论文不存在"));

        if (paper.getSubmitterId() == null || !paper.getSubmitterId().equals(currentUserId)) {
            throw new BusinessException("无权限访问该论文的检测结果");
        }
    }

    private boolean isAdmin(String role) {
        return User.UserRole.ROLE_ADMIN.name().equals(role);
    }

    private Long getCurrentUserId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.getDetails() instanceof Long) {
            return (Long) authentication.getDetails();
        }
        throw new BusinessException("用户未登录");
    }

    private String getCurrentUserRole() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && !authentication.getAuthorities().isEmpty()) {
            return authentication.getAuthorities().iterator().next().getAuthority();
        }
        throw new BusinessException("用户未登录");
    }

    private DuplicateCheckReport getLatestReport(Long paperId) {
        List<DuplicateCheckReport> history = duplicateDetectionService.getCheckHistory(paperId);
        return history.isEmpty() ? null : history.get(0);
    }
}
