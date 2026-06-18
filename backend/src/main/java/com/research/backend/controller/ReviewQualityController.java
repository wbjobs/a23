package com.research.backend.controller;

import com.research.backend.common.Result;
import com.research.backend.dto.QualityCalculateRequest;
import com.research.backend.dto.QualityTrendDto;
import com.research.backend.dto.ReviewQualityDashboardDto;
import com.research.backend.dto.ReviewerDetailQualityDto;
import com.research.backend.dto.ReviewerRankingDto;
import com.research.backend.entity.ReviewQualityMetrics;
import com.research.backend.entity.User;
import com.research.backend.exception.BusinessException;
import com.research.backend.service.ReviewQualityService;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.io.PrintWriter;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/quality")
@RequiredArgsConstructor
public class ReviewQualityController {

    private final ReviewQualityService reviewQualityService;

    @GetMapping("/dashboard")
    @PreAuthorize("hasAnyRole('ADMIN', 'REVIEWER')")
    public Result<ReviewQualityDashboardDto> getDashboardData() {
        ReviewQualityDashboardDto data = reviewQualityService.getDashboardData();
        return Result.success(data);
    }

    @GetMapping("/reviewer/{reviewerId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'REVIEWER')")
    public Result<ReviewerDetailQualityDto> getReviewerQuality(@PathVariable Long reviewerId) {
        checkPermission(reviewerId);
        ReviewerDetailQualityDto data = reviewQualityService.getReviewerQuality(reviewerId);
        return Result.success(data);
    }

    @GetMapping("/reviewer/ranking")
    @PreAuthorize("hasAnyRole('ADMIN', 'REVIEWER')")
    public Result<List<ReviewerRankingDto>> getReviewerRanking(
            @RequestParam(required = false, defaultValue = "qualityScore") String sortBy,
            @RequestParam(required = false, defaultValue = "20") Integer limit) {
        List<ReviewerRankingDto> rankings = reviewQualityService.getReviewerRanking(sortBy, limit);
        return Result.success(rankings);
    }

    @GetMapping("/distribution/score")
    @PreAuthorize("hasAnyRole('ADMIN', 'REVIEWER')")
    public Result<Map<String, Object>> getScoreDistribution(
            @RequestParam(required = false, defaultValue = "12") Integer periodMonths) {
        Map<String, Object> distribution = reviewQualityService.getScoreDistribution(periodMonths);
        return Result.success(distribution);
    }

    @GetMapping("/distribution/duration")
    @PreAuthorize("hasAnyRole('ADMIN', 'REVIEWER')")
    public Result<Map<String, Object>> getDurationDistribution(
            @RequestParam(required = false, defaultValue = "12") Integer periodMonths) {
        Map<String, Object> distribution = reviewQualityService.getDurationDistribution(periodMonths);
        return Result.success(distribution);
    }

    @GetMapping("/trend")
    @PreAuthorize("hasAnyRole('ADMIN', 'REVIEWER')")
    public Result<List<QualityTrendDto>> getTrendData(
            @RequestParam(required = false, defaultValue = "6") Integer months) {
        List<QualityTrendDto> trends = reviewQualityService.getTrendData(months);
        return Result.success(trends);
    }

    @PostMapping("/calculate")
    @PreAuthorize("hasRole('ADMIN')")
    public Result<String> calculateQualityMetrics(@Valid @RequestBody QualityCalculateRequest request) {
        reviewQualityService.batchCalculate(request.getReviewerIds(), request.getPeriod());
        return Result.success("质量指标计算完成");
    }

    @GetMapping("/calculate/{reviewerId}")
    @PreAuthorize("hasRole('ADMIN')")
    public Result<ReviewQualityMetrics> calculateReviewerMetrics(
            @PathVariable Long reviewerId,
            @RequestParam(required = false, defaultValue = "12") Integer periodMonths) {
        ReviewQualityMetrics metrics = reviewQualityService.calculateMetrics(reviewerId, periodMonths);
        return Result.success("计算完成", metrics);
    }

    @GetMapping("/export")
    @PreAuthorize("hasRole('ADMIN')")
    public void exportQualityReport(HttpServletResponse response,
                                    @RequestParam(required = false, defaultValue = "12") Integer periodMonths) throws IOException {
        List<ReviewerRankingDto> rankings = reviewQualityService.getReviewerRanking("qualityScore", 100);

        response.setContentType("text/csv;charset=utf-8");
        response.setHeader("Content-Disposition", "attachment; filename=quality_report.csv");

        PrintWriter writer = response.getWriter();
        writer.println('\uFEFF' + "排名,评审人ID,评审人姓名,所属机构,评审数量,平均时长(小时),及时完成率(%),评分偏离度,综合质量分,质量等级");

        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
        for (ReviewerRankingDto dto : rankings) {
            writer.println(String.format("%d,%d,%s,%s,%d,%.2f,%.2f,%.2f,%.2f,%s",
                    dto.getRank(),
                    dto.getReviewerId(),
                    escapeCsv(dto.getReviewerName()),
                    escapeCsv(dto.getAffiliation()),
                    dto.getReviewCount(),
                    dto.getAverageDuration() != null ? dto.getAverageDuration() : 0.0,
                    dto.getTimelyRate() != null ? dto.getTimelyRate() : 0.0,
                    dto.getScoreDeviation() != null ? dto.getScoreDeviation() : 0.0,
                    dto.getOverallQualityScore() != null ? dto.getOverallQualityScore() : 0.0,
                    dto.getQualityLevel()));
        }

        writer.flush();
        writer.close();
    }

    private void checkPermission(Long reviewerId) {
        String role = getCurrentUserRole();
        if (!isAdmin(role)) {
            Long currentUserId = getCurrentUserId();
            if (!currentUserId.equals(reviewerId)) {
                throw new BusinessException("无权查看其他评审人的质量数据");
            }
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

    private String escapeCsv(String value) {
        if (value == null) return "";
        if (value.contains(",") || value.contains("\"") || value.contains("\n")) {
            return "\"" + value.replace("\"", "\"\"") + "\"";
        }
        return value;
    }
}
