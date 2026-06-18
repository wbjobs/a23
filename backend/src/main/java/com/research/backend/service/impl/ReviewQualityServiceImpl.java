package com.research.backend.service.impl;

import com.research.backend.dto.CommentsQualityDto;
import com.research.backend.dto.QualityTrendDto;
import com.research.backend.dto.ReviewQualityDashboardDto;
import com.research.backend.dto.ReviewRecordQualityDto;
import com.research.backend.dto.ReviewerDetailQualityDto;
import com.research.backend.dto.ReviewerRankingDto;
import com.research.backend.entity.ReviewQualityMetrics;
import com.research.backend.entity.ReviewRecord;
import com.research.backend.neo4j.entity.Paper;
import com.research.backend.neo4j.entity.Reviewer;
import com.research.backend.neo4j.repository.PaperRepository;
import com.research.backend.neo4j.repository.ReviewerRepository;
import com.research.backend.repository.ReviewQualityMetricsRepository;
import com.research.backend.repository.ReviewRecordRepository;
import com.research.backend.service.ReviewQualityService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class ReviewQualityServiceImpl implements ReviewQualityService {

    private final ReviewRecordRepository reviewRecordRepository;
    private final ReviewQualityMetricsRepository reviewQualityMetricsRepository;
    private final ReviewerRepository reviewerRepository;
    private final PaperRepository paperRepository;

    private static final double TIMELY_HOURS_THRESHOLD = 168.0;
    private static final List<String> DURATION_BUCKETS = Arrays.asList("0-1天", "1-3天", "3-7天", ">7天");
    private static final List<String> SCORE_BUCKETS = Arrays.asList("0-25", "25-50", "50-75", "75-100");
    private static final Pattern NUMBERED_LIST_PATTERN = Pattern.compile("^\\s*[1-9][0-9]*[.、)）]\\s", Pattern.MULTILINE);
    private static final Pattern SUGGESTION_PATTERN = Pattern.compile("(建议|应该|需要|可以|考虑|推荐)");

    @Override
    public ReviewQualityDashboardDto getDashboardData() {
        List<ReviewRecord> allSubmittedReviews = reviewRecordRepository
                .findByStatusAndReviewTimeAfter(ReviewRecord.ReviewStatus.SUBMITTED, LocalDateTime.now().minusMonths(12));

        Set<Long> reviewerIds = allSubmittedReviews.stream()
                .map(ReviewRecord::getReviewerId)
                .collect(Collectors.toSet());
        Set<Long> paperIds = allSubmittedReviews.stream()
                .map(ReviewRecord::getPaperId)
                .collect(Collectors.toSet());

        double avgDuration = calculateAverageDuration(allSubmittedReviews);
        double timelyRate = calculateTimelyRate(allSubmittedReviews);
        double scoreDeviation = calculateOverallScoreDeviation(allSubmittedReviews);

        List<ReviewerRankingDto> rankings = getReviewerRanking("qualityScore", 10);
        Map<String, Integer> durationDist = getDurationDistributionInternal(allSubmittedReviews);
        Map<String, Integer> scoreDist = getScoreDistributionInternal(allSubmittedReviews);
        List<QualityTrendDto> trends = getTrendData(6);

        return ReviewQualityDashboardDto.builder()
                .totalReviewers(reviewerIds.size())
                .totalPapers(paperIds.size())
                .totalReviews(allSubmittedReviews.size())
                .overallAverageDuration(avgDuration)
                .overallTimelyRate(timelyRate)
                .overallScoreDeviation(scoreDeviation)
                .reviewerRankings(rankings)
                .durationDistribution(durationDist)
                .scoreDistribution(scoreDist)
                .trendData(trends)
                .build();
    }

    @Override
    public ReviewerDetailQualityDto getReviewerQuality(Long reviewerId) {
        Reviewer reviewer = reviewerRepository.findById(reviewerId)
                .orElseThrow(() -> new RuntimeException("评审人不存在"));

        ReviewQualityMetrics latestMetrics = reviewQualityMetricsRepository
                .findTopByReviewerIdOrderByCalculateTimeDesc(reviewerId)
                .orElse(null);

        List<ReviewRecord> reviews = reviewRecordRepository
                .findByReviewerIdAndStatus(reviewerId, ReviewRecord.ReviewStatus.SUBMITTED);

        List<ReviewRecordQualityDto> reviewDtos = convertToReviewQualityDtos(reviews);
        CommentsQualityDto commentsQuality = calculateCommentsQuality(reviews);

        return ReviewerDetailQualityDto.builder()
                .reviewer(reviewer)
                .latestMetrics(latestMetrics)
                .reviews(reviewDtos)
                .commentsQualityAnalysis(commentsQuality)
                .build();
    }

    @Override
    @Transactional
    public ReviewQualityMetrics calculateMetrics(Long reviewerId, Integer periodMonths) {
        Reviewer reviewer = reviewerRepository.findById(reviewerId)
                .orElseThrow(() -> new RuntimeException("评审人不存在"));

        LocalDateTime startTime = LocalDateTime.now().minusMonths(periodMonths);
        List<ReviewRecord> reviews = reviewRecordRepository
                .findByReviewerIdAndStatusAndReviewTimeAfter(
                        reviewerId,
                        ReviewRecord.ReviewStatus.SUBMITTED,
                        startTime);

        if (reviews.isEmpty()) {
            throw new RuntimeException("该评审人在指定周期内没有评审记录");
        }

        List<Double> durations = reviews.stream()
                .map(this::calculateDurationHours)
                .filter(d -> d != null)
                .collect(Collectors.toList());

        double avgDuration = durations.stream().mapToDouble(Double::doubleValue).average().orElse(0.0);
        double medianDuration = calculateMedian(durations);

        long timelyCount = durations.stream().filter(d -> d <= TIMELY_HOURS_THRESHOLD).count();
        long overdueCount = durations.size() - timelyCount;
        double timelyRate = durations.isEmpty() ? 0.0 : (double) timelyCount / durations.size() * 100;

        double avgOverallScore = reviews.stream()
                .mapToInt(r -> r.getOverallScore() != null ? r.getOverallScore() : 0)
                .average().orElse(0.0);
        double avgInnovationScore = reviews.stream()
                .mapToInt(r -> r.getInnovationScore() != null ? r.getInnovationScore() : 0)
                .average().orElse(0.0);
        double avgMethodScore = reviews.stream()
                .mapToInt(r -> r.getMethodScore() != null ? r.getMethodScore() : 0)
                .average().orElse(0.0);
        double avgWritingScore = reviews.stream()
                .mapToInt(r -> r.getWritingScore() != null ? r.getWritingScore() : 0)
                .average().orElse(0.0);

        double[] deviationResult = calculateScoreDeviation(reviewerId, reviews);
        double stdDev = deviationResult[0];
        double coefficientOfVariation = deviationResult[1];

        double avgWordCount = reviews.stream()
                .mapToInt(r -> r.getComments() != null ? r.getComments().length() : 0)
                .average().orElse(0.0);

        double commentDetailScore = calculateCommentDetailScore(reviews);

        double overallQualityScore = calculateOverallQualityScore(
                timelyRate, stdDev, avgDuration, commentDetailScore, avgOverallScore);

        String qualityLevel = determineQualityLevel(overallQualityScore);

        ReviewQualityMetrics metrics = ReviewQualityMetrics.builder()
                .reviewerId(reviewerId)
                .reviewerName(reviewer.getName())
                .period(periodMonths)
                .calculateTime(LocalDateTime.now())
                .reviewCount(reviews.size())
                .averageOverallScore(avgOverallScore)
                .averageInnovationScore(avgInnovationScore)
                .averageMethodScore(avgMethodScore)
                .averageWritingScore(avgWritingScore)
                .averageReviewDurationHours(avgDuration)
                .medianReviewDurationHours(medianDuration)
                .scoreStandardDeviation(stdDev)
                .scoreCoefficientOfVariation(coefficientOfVariation)
                .timelyCompletedCount((int) timelyCount)
                .overDueCount((int) overdueCount)
                .timelyCompletionRate(timelyRate)
                .rejectedByAuthorCount(0)
                .acceptedByAuthorCount(0)
                .commentWordCountAverage(avgWordCount)
                .commentDetailScore(commentDetailScore)
                .overallQualityScore(overallQualityScore)
                .qualityLevel(qualityLevel)
                .build();

        ReviewQualityMetrics saved = reviewQualityMetricsRepository.save(metrics);

        updateReviewerQualityFields(reviewer, metrics);

        return saved;
    }

    @Override
    public List<ReviewerRankingDto> getReviewerRanking(String sortBy, Integer limit) {
        if (limit == null || limit <= 0) {
            limit = 20;
        }

        List<ReviewQualityMetrics> allMetrics = reviewQualityMetricsRepository
                .findByPeriodOrderByOverallQualityScoreDesc(1);

        Map<Long, ReviewQualityMetrics> latestMetricsMap = new HashMap<>();
        for (ReviewQualityMetrics metrics : allMetrics) {
            if (!latestMetricsMap.containsKey(metrics.getReviewerId())) {
                latestMetricsMap.put(metrics.getReviewerId(), metrics);
            }
        }

        List<ReviewerRankingDto> rankings = new ArrayList<>();
        int rank = 1;

        List<ReviewQualityMetrics> sortedMetrics = new ArrayList<>(latestMetricsMap.values());

        switch (sortBy != null ? sortBy : "qualityScore") {
            case "reviewSpeed":
                sortedMetrics.sort(Comparator.comparingDouble(
                        m -> m.getAverageReviewDurationHours() != null ? m.getAverageReviewDurationHours() : Double.MAX_VALUE));
                break;
            case "consistency":
                sortedMetrics.sort(Comparator.comparingDouble(
                        m -> m.getScoreStandardDeviation() != null ? m.getScoreStandardDeviation() : Double.MAX_VALUE));
                break;
            case "timelyRate":
                sortedMetrics.sort(Comparator.comparingDouble(
                        m -> m.getTimelyCompletionRate() != null ? -m.getTimelyCompletionRate() : 0));
                break;
            case "qualityScore":
            default:
                sortedMetrics.sort(Comparator.comparingDouble(
                        m -> m.getOverallQualityScore() != null ? -m.getOverallQualityScore() : 0));
                break;
        }

        for (ReviewQualityMetrics metrics : sortedMetrics) {
            Reviewer reviewer = reviewerRepository.findById(metrics.getReviewerId()).orElse(null);
            if (reviewer == null) continue;

            ReviewerRankingDto dto = ReviewerRankingDto.builder()
                    .reviewerId(metrics.getReviewerId())
                    .reviewerName(metrics.getReviewerName())
                    .affiliation(reviewer.getAffiliation())
                    .reviewCount(metrics.getReviewCount())
                    .averageDuration(metrics.getAverageReviewDurationHours())
                    .timelyRate(metrics.getTimelyCompletionRate())
                    .scoreDeviation(metrics.getScoreStandardDeviation())
                    .overallQualityScore(metrics.getOverallQualityScore())
                    .qualityLevel(metrics.getQualityLevel())
                    .rank(rank++)
                    .build();
            rankings.add(dto);

            if (rankings.size() >= limit) break;
        }

        return rankings;
    }

    @Override
    public Map<String, Object> getScoreDistribution(Integer periodMonths) {
        LocalDateTime startTime = LocalDateTime.now().minusMonths(periodMonths != null ? periodMonths : 12);
        List<ReviewRecord> reviews = reviewRecordRepository
                .findByStatusAndReviewTimeAfter(ReviewRecord.ReviewStatus.SUBMITTED, startTime);
        return Map.of("distribution", getScoreDistributionInternal(reviews), "total", reviews.size());
    }

    @Override
    public Map<String, Object> getDurationDistribution(Integer periodMonths) {
        LocalDateTime startTime = LocalDateTime.now().minusMonths(periodMonths != null ? periodMonths : 12);
        List<ReviewRecord> reviews = reviewRecordRepository
                .findByStatusAndReviewTimeAfter(ReviewRecord.ReviewStatus.SUBMITTED, startTime);
        return Map.of("distribution", getDurationDistributionInternal(reviews), "total", reviews.size());
    }

    @Override
    public List<QualityTrendDto> getTrendData(Integer months) {
        int monthsCount = months != null ? months : 6;
        List<QualityTrendDto> trends = new ArrayList<>();
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM");

        for (int i = monthsCount - 1; i >= 0; i--) {
            YearMonth yearMonth = YearMonth.now().minusMonths(i);
            LocalDateTime startOfMonth = yearMonth.atDay(1).atStartOfDay();
            LocalDateTime endOfMonth = yearMonth.atEndOfMonth().atTime(23, 59, 59);

            List<ReviewRecord> monthlyReviews = reviewRecordRepository
                    .findByStatusAndReviewTimeAfter(ReviewRecord.ReviewStatus.SUBMITTED, startOfMonth)
                    .stream()
                    .filter(r -> r.getReviewTime() != null && !r.getReviewTime().isAfter(endOfMonth))
                    .collect(Collectors.toList());

            double avgDuration = calculateAverageDuration(monthlyReviews);
            double avgScore = monthlyReviews.stream()
                    .mapToInt(r -> r.getOverallScore() != null ? r.getOverallScore() : 0)
                    .average().orElse(0.0);
            double timelyRate = calculateTimelyRate(monthlyReviews);

            trends.add(QualityTrendDto.builder()
                    .month(yearMonth.format(formatter))
                    .averageDuration(avgDuration)
                    .averageScore(avgScore)
                    .timelyRate(timelyRate)
                    .build());
        }

        return trends;
    }

    @Override
    @Transactional
    public void batchCalculate(List<Long> reviewerIds, Integer periodMonths) {
        if (reviewerIds == null || reviewerIds.isEmpty()) {
            reviewerIds = reviewerRepository.findAll().stream()
                    .map(Reviewer::getId)
                    .collect(Collectors.toList());
        }

        for (Long reviewerId : reviewerIds) {
            try {
                calculateMetrics(reviewerId, periodMonths);
            } catch (Exception e) {
                log.warn("计算评审人 {} 质量指标失败: {}", reviewerId, e.getMessage());
            }
        }
    }

    private double calculateDurationHours(ReviewRecord record) {
        if (record.getAssignTime() == null || record.getReviewTime() == null) {
            return null;
        }
        return Duration.between(record.getAssignTime(), record.getReviewTime()).toMinutes() / 60.0;
    }

    private double calculateAverageDuration(List<ReviewRecord> reviews) {
        return reviews.stream()
                .map(this::calculateDurationHours)
                .filter(d -> d != null)
                .mapToDouble(Double::doubleValue)
                .average()
                .orElse(0.0);
    }

    private double calculateTimelyRate(List<ReviewRecord> reviews) {
        List<Double> durations = reviews.stream()
                .map(this::calculateDurationHours)
                .filter(d -> d != null)
                .collect(Collectors.toList());
        if (durations.isEmpty()) return 0.0;
        long timelyCount = durations.stream().filter(d -> d <= TIMELY_HOURS_THRESHOLD).count();
        return (double) timelyCount / durations.size() * 100;
    }

    private double calculateOverallScoreDeviation(List<ReviewRecord> reviews) {
        if (reviews.size() < 2) return 0.0;
        double mean = reviews.stream()
                .mapToInt(r -> r.getOverallScore() != null ? r.getOverallScore() : 0)
                .average().orElse(0.0);
        double variance = reviews.stream()
                .mapToDouble(r -> Math.pow((r.getOverallScore() != null ? r.getOverallScore() : 0) - mean, 2))
                .average().orElse(0.0);
        return Math.sqrt(variance);
    }

    private double[] calculateScoreDeviation(Long reviewerId, List<ReviewRecord> reviews) {
        List<Double> deviations = new ArrayList<>();
        List<Integer> reviewerScores = new ArrayList<>();

        for (ReviewRecord review : reviews) {
            if (review.getOverallScore() == null) continue;

            List<ReviewRecord> otherReviews = reviewRecordRepository
                    .findByPaperIdAndReviewerIdNotAndStatus(
                            review.getPaperId(),
                            reviewerId,
                            ReviewRecord.ReviewStatus.SUBMITTED);

            if (otherReviews.isEmpty()) continue;

            double otherMean = otherReviews.stream()
                    .mapToInt(r -> r.getOverallScore() != null ? r.getOverallScore() : 0)
                    .average().orElse(0.0);

            double deviation = review.getOverallScore() - otherMean;
            deviations.add(deviation);
            reviewerScores.add(review.getOverallScore());
        }

        if (deviations.size() < 3) {
            return new double[]{0.0, 0.0};
        }

        double meanDev = deviations.stream().mapToDouble(Double::doubleValue).average().orElse(0.0);
        double variance = deviations.stream()
                .mapToDouble(d -> Math.pow(d - meanDev, 2))
                .average().orElse(0.0);
        double stdDev = Math.sqrt(variance);

        double reviewerMeanScore = reviewerScores.stream().mapToInt(Integer::intValue).average().orElse(1.0);
        double coefficientOfVariation = reviewerMeanScore > 0 ? stdDev / reviewerMeanScore : 0.0;

        return new double[]{stdDev, coefficientOfVariation};
    }

    private double calculateMedian(List<Double> values) {
        if (values.isEmpty()) return 0.0;
        List<Double> sorted = new ArrayList<>(values);
        Collections.sort(sorted);
        int middle = sorted.size() / 2;
        if (sorted.size() % 2 == 0) {
            return (sorted.get(middle - 1) + sorted.get(middle)) / 2.0;
        } else {
            return sorted.get(middle);
        }
    }

    private Map<String, Integer> getDurationDistributionInternal(List<ReviewRecord> reviews) {
        Map<String, Integer> distribution = new LinkedHashMap<>();
        for (String bucket : DURATION_BUCKETS) {
            distribution.put(bucket, 0);
        }

        for (ReviewRecord review : reviews) {
            Double duration = calculateDurationHours(review);
            if (duration == null) continue;

            String bucket;
            if (duration <= 24) bucket = "0-1天";
            else if (duration <= 72) bucket = "1-3天";
            else if (duration <= 168) bucket = "3-7天";
            else bucket = ">7天";

            distribution.put(bucket, distribution.get(bucket) + 1);
        }

        return distribution;
    }

    private Map<String, Integer> getScoreDistributionInternal(List<ReviewRecord> reviews) {
        Map<String, Integer> distribution = new LinkedHashMap<>();
        for (String bucket : SCORE_BUCKETS) {
            distribution.put(bucket, 0);
        }

        for (ReviewRecord review : reviews) {
            Integer score = review.getOverallScore();
            if (score == null) continue;

            String bucket;
            if (score < 25) bucket = "0-25";
            else if (score < 50) bucket = "25-50";
            else if (score < 75) bucket = "50-75";
            else bucket = "75-100";

            distribution.put(bucket, distribution.get(bucket) + 1);
        }

        return distribution;
    }

    private double calculateCommentDetailScore(List<ReviewRecord> reviews) {
        if (reviews.isEmpty()) return 0.0;

        double totalScore = 0.0;
        for (ReviewRecord review : reviews) {
            String comments = review.getComments();
            if (comments == null || comments.isEmpty()) continue;

            double wordCountScore = Math.min(comments.length() / 200.0, 1.0) * 50;

            boolean hasStructure = NUMBERED_LIST_PATTERN.matcher(comments).find();
            boolean hasSuggestions = SUGGESTION_PATTERN.matcher(comments).find();

            double structureScore = (hasStructure ? 25 : 0) + (hasSuggestions ? 25 : 0);

            totalScore += wordCountScore + structureScore;
        }

        return totalScore / reviews.size();
    }

    private double calculateOverallQualityScore(double timelyRate, double stdDev,
                                                double avgDuration, double commentDetailScore,
                                                double avgOverallScore) {
        double timelyScore = calculateTimelyScore(timelyRate);
        double consistencyScore = calculateConsistencyScore(stdDev);
        double speedScore = calculateSpeedScore(avgDuration);
        double commentScore = Math.min(commentDetailScore, 100.0);

        return (timelyScore * 0.25) + (consistencyScore * 0.25) + (speedScore * 0.25) + (commentScore * 0.25);
    }

    private double calculateTimelyScore(double timelyRate) {
        if (timelyRate >= 90) return 100;
        if (timelyRate >= 70) return 80;
        if (timelyRate >= 50) return 60;
        if (timelyRate >= 30) return 40;
        return 20;
    }

    private double calculateConsistencyScore(double stdDev) {
        if (stdDev <= 5) return 100;
        if (stdDev <= 10) return 80;
        if (stdDev <= 15) return 60;
        if (stdDev <= 20) return 40;
        return 20;
    }

    private double calculateSpeedScore(double avgDurationHours) {
        if (avgDurationHours < 2) return 70;
        if (avgDurationHours <= 24) return 100;
        if (avgDurationHours <= 72) return 80;
        if (avgDurationHours <= 168) return 60;
        if (avgDurationHours <= 336) return 40;
        return 20;
    }

    private String determineQualityLevel(double overallScore) {
        if (overallScore >= 85) return "EXCELLENT";
        if (overallScore >= 70) return "GOOD";
        if (overallScore >= 55) return "FAIR";
        return "POOR";
    }

    private void updateReviewerQualityFields(Reviewer reviewer, ReviewQualityMetrics metrics) {
        reviewer.setTotalReviews(metrics.getReviewCount());
        reviewer.setAverageScore(metrics.getAverageOverallScore());
        reviewer.setAverageReviewDurationHours(metrics.getAverageReviewDurationHours());
        reviewer.setScoreDeviation(metrics.getScoreStandardDeviation());
        reviewer.setTimelyCompletionRate(metrics.getTimelyCompletionRate() != null ? metrics.getTimelyCompletionRate().intValue() : null);
        reviewer.setQualityLastCalculated(LocalDateTime.now());
        reviewerRepository.save(reviewer);
    }

    private List<ReviewRecordQualityDto> convertToReviewQualityDtos(List<ReviewRecord> reviews) {
        List<ReviewRecordQualityDto> dtos = new ArrayList<>();
        for (ReviewRecord review : reviews) {
            Paper paper = paperRepository.findById(review.getPaperId()).orElse(null);
            String paperTitle = paper != null ? paper.getTitle() : "未知论文";

            Double deviation = null;
            List<ReviewRecord> otherReviews = reviewRecordRepository
                    .findByPaperIdAndReviewerIdNotAndStatus(
                            review.getPaperId(),
                            review.getReviewerId(),
                            ReviewRecord.ReviewStatus.SUBMITTED);
            if (!otherReviews.isEmpty() && review.getOverallScore() != null) {
                double otherMean = otherReviews.stream()
                        .mapToInt(r -> r.getOverallScore() != null ? r.getOverallScore() : 0)
                        .average().orElse(0.0);
                deviation = review.getOverallScore() - otherMean;
            }

            Double duration = calculateDurationHours(review);

            dtos.add(ReviewRecordQualityDto.builder()
                    .id(review.getId())
                    .paperId(review.getPaperId())
                    .paperTitle(paperTitle)
                    .innovationScore(review.getInnovationScore())
                    .methodScore(review.getMethodScore())
                    .writingScore(review.getWritingScore())
                    .overallScore(review.getOverallScore())
                    .reviewDurationHours(duration)
                    .timely(duration != null && duration <= TIMELY_HOURS_THRESHOLD)
                    .commentLength(review.getComments() != null ? review.getComments().length() : 0)
                    .deviationFromMean(deviation)
                    .build());
        }
        return dtos;
    }

    private CommentsQualityDto calculateCommentsQuality(List<ReviewRecord> reviews) {
        if (reviews.isEmpty()) {
            return CommentsQualityDto.builder()
                    .averageWordCount(0.0)
                    .structureScore(0.0)
                    .detailLevel(0.0)
                    .actionableAdviceRate(0.0)
                    .build();
        }

        double avgWordCount = reviews.stream()
                .mapToInt(r -> r.getComments() != null ? r.getComments().length() : 0)
                .average().orElse(0.0);

        long structuredCount = reviews.stream()
                .filter(r -> r.getComments() != null && NUMBERED_LIST_PATTERN.matcher(r.getComments()).find())
                .count();

        long suggestionCount = reviews.stream()
                .filter(r -> r.getComments() != null && SUGGESTION_PATTERN.matcher(r.getComments()).find())
                .count();

        double structureScore = (double) structuredCount / reviews.size() * 100;
        double detailLevel = Math.min(avgWordCount / 3.0, 100.0);
        double actionableRate = (double) suggestionCount / reviews.size() * 100;

        return CommentsQualityDto.builder()
                .averageWordCount(avgWordCount)
                .structureScore(structureScore)
                .detailLevel(detailLevel)
                .actionableAdviceRate(actionableRate)
                .build();
    }
}
