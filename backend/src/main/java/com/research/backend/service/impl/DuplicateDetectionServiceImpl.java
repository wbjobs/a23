package com.research.backend.service.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.research.backend.dto.*;
import com.research.backend.entity.DuplicateCheckReport;
import com.research.backend.exception.BusinessException;
import com.research.backend.neo4j.entity.*;
import com.research.backend.neo4j.repository.PaperRepository;
import com.research.backend.repository.DuplicateCheckReportRepository;
import com.research.backend.service.DuplicateDetectionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class DuplicateDetectionServiceImpl implements DuplicateDetectionService {

    private final PaperRepository paperRepository;
    private final DuplicateCheckReportRepository duplicateCheckReportRepository;
    private final ObjectMapper objectMapper;

    private static final double DATASET_WEIGHT = 0.4;
    private static final double KEYWORD_WEIGHT = 0.3;
    private static final double REFERENCE_WEIGHT = 0.2;
    private static final double AUTHOR_WEIGHT = 0.1;
    private static final double SIMILARITY_THRESHOLD = 0.6;

    @Override
    @Transactional
    public DuplicateCheckResult checkPaper(Long paperId) {
        log.info("开始检测论文不端，paperId: {}", paperId);

        Paper targetPaper = paperRepository.findById(paperId)
                .orElseThrow(() -> new BusinessException("论文不存在"));

        targetPaper.setDuplicateStatus("PROCESSING");
        paperRepository.save(targetPaper);

        try {
            Set<String> targetDatasets = getDatasetNames(targetPaper);
            Set<String> targetKeywords = getKeywordNames(targetPaper);
            Set<String> targetReferences = getReferenceTitles(targetPaper);
            Set<String> targetAuthors = getAuthorNames(targetPaper);

            List<Paper> allPapers = paperRepository.findAll();
            List<SimilarPaperDto> similarPapers = new ArrayList<>();
            List<SimilarityEdge> similarityEdges = new ArrayList<>();
            double maxSimilarity = 0.0;

            for (Paper otherPaper : allPapers) {
                if (otherPaper.getId().equals(paperId)) {
                    continue;
                }

                Set<String> otherDatasets = getDatasetNames(otherPaper);
                Set<String> otherKeywords = getKeywordNames(otherPaper);
                Set<String> otherReferences = getReferenceTitles(otherPaper);
                Set<String> otherAuthors = getAuthorNames(otherPaper);

                double datasetSim = calculateJaccardSimilarity(targetDatasets, otherDatasets);
                double keywordSim = calculateJaccardSimilarity(targetKeywords, otherKeywords);
                double referenceSim = calculateJaccardSimilarity(targetReferences, otherReferences);
                double authorSim = calculateJaccardSimilarity(targetAuthors, otherAuthors);

                double overallSim = DATASET_WEIGHT * datasetSim +
                        KEYWORD_WEIGHT * keywordSim +
                        REFERENCE_WEIGHT * referenceSim +
                        AUTHOR_WEIGHT * authorSim;

                if (overallSim > maxSimilarity) {
                    maxSimilarity = overallSim;
                }

                if (overallSim >= SIMILARITY_THRESHOLD) {
                    List<String> overlappingDatasets = getIntersection(targetDatasets, otherDatasets);
                    List<String> overlappingKeywords = getIntersection(targetKeywords, otherKeywords);
                    List<String> overlappingReferences = getIntersection(targetReferences, otherReferences);

                    String similarityType = determineSimilarityType(datasetSim, keywordSim, referenceSim, authorSim);
                    String riskLevel = determineRiskLevel(overallSim);
                    double overlapRatio = (overlappingDatasets.size() + overlappingKeywords.size() + overlappingReferences.size()) /
                            (double) (targetDatasets.size() + targetKeywords.size() + targetReferences.size() + 1);

                    SimilarityEdge edge = new SimilarityEdge();
                    edge.setTargetPaper(otherPaper);
                    edge.setSimilarityScore(overallSim);
                    edge.setSimilarityType(similarityType);
                    edge.setOverlappingDatasets(overlappingDatasets);
                    edge.setOverlappingKeywords(overlappingKeywords);
                    edge.setOverlappingReferences(overlappingReferences);
                    edge.setOverlapRatio(overlapRatio);
                    edge.setRiskLevel(riskLevel);
                    edge.setCheckTime(LocalDateTime.now());
                    similarityEdges.add(edge);

                    SimilarPaperDto similarPaperDto = SimilarPaperDto.builder()
                            .paperId(otherPaper.getId())
                            .title(otherPaper.getTitle())
                            .authors(String.join(", ", otherAuthors))
                            .similarityScore(overallSim)
                            .similarityType(similarityType)
                            .overlappingDatasets(overlappingDatasets)
                            .overlappingKeywords(overlappingKeywords)
                            .riskLevel(riskLevel)
                            .build();
                    similarPapers.add(similarPaperDto);
                }
            }

            similarPapers.sort((a, b) -> Double.compare(b.getSimilarityScore(), a.getSimilarityScore()));

            targetPaper.setSimilarPapers(similarityEdges);
            targetPaper.setDuplicateCheckFlag(true);
            targetPaper.setDuplicateCheckTime(LocalDateTime.now());
            targetPaper.setOverallSimilarity(maxSimilarity);
            targetPaper.setDuplicateRiskLevel(determineRiskLevel(maxSimilarity));
            targetPaper.setDuplicateStatus("COMPLETED");
            paperRepository.save(targetPaper);

            SausageDetectionDto sausageDetection = detectSausagePapers(paperId);

            Map<String, Object> evidenceDetails = buildEvidenceDetails(targetPaper, similarPapers, sausageDetection);

            DuplicateCheckResult result = DuplicateCheckResult.builder()
                    .paperId(paperId)
                    .overallSimilarity(maxSimilarity)
                    .overallRiskLevel(determineRiskLevel(maxSimilarity))
                    .similarPapers(similarPapers)
                    .sausageDetection(sausageDetection)
                    .evidenceDetails(evidenceDetails)
                    .checkTime(LocalDateTime.now())
                    .build();

            saveDuplicateCheckReport(targetPaper, result, sausageDetection, evidenceDetails);

            log.info("论文不端检测完成，paperId: {}, 最大相似度: {}, 风险等级: {}",
                    paperId, maxSimilarity, determineRiskLevel(maxSimilarity));

            return result;

        } catch (Exception e) {
            log.error("论文不端检测失败，paperId: {}", paperId, e);
            targetPaper.setDuplicateStatus("FAILED");
            paperRepository.save(targetPaper);
            throw new BusinessException("论文不端检测失败: " + e.getMessage());
        }
    }

    @Override
    public List<DuplicateCheckResult> batchCheck(List<Long> paperIds) {
        log.info("开始批量检测论文不端，paperIds: {}", paperIds);
        List<DuplicateCheckResult> results = new ArrayList<>();
        for (Long paperId : paperIds) {
            try {
                results.add(checkPaper(paperId));
            } catch (Exception e) {
                log.error("批量检测论文失败，paperId: {}", paperId, e);
            }
        }
        return results;
    }

    @Override
    public List<DuplicateCheckReport> getCheckHistory(Long paperId) {
        return duplicateCheckReportRepository.findByPaperIdOrderByCheckTimeDesc(paperId);
    }

    @Override
    @Transactional
    public DuplicateCheckReport updateReportStatus(Long reportId, String status, String note) {
        DuplicateCheckReport report = duplicateCheckReportRepository.findById(reportId)
                .orElseThrow(() -> new BusinessException("报告不存在"));
        report.setReportStatus(status);
        report.setCheckNote(note);
        return duplicateCheckReportRepository.save(report);
    }

    @Override
    public SausageDetectionDto detectSausagePapers(Long paperId) {
        Paper targetPaper = paperRepository.findById(paperId)
                .orElseThrow(() -> new BusinessException("论文不存在"));

        Set<String> targetDatasets = getDatasetNames(targetPaper);
        Set<String> targetKeywords = getKeywordNames(targetPaper);
        Set<String> targetAuthors = getAuthorNames(targetPaper);

        List<Map<String, Object>> clusteringData = paperRepository.findAllPapersForClustering(paperId);

        Map<String, List<Long>> datasetPaperMap = new HashMap<>();
        Map<Long, Set<String>> paperKeywordsMap = new HashMap<>();
        Map<Long, Set<String>> paperAuthorsMap = new HashMap<>();

        for (Map<String, Object> data : clusteringData) {
            Paper paper = (Paper) data.get("p");
            if (paper == null) continue;

            @SuppressWarnings("unchecked")
            List<String> datasets = (List<String>) data.get("collect(DISTINCT d.name)");
            @SuppressWarnings("unchecked")
            List<String> keywords = (List<String>) data.get("collect(DISTINCT k.name)");

            if (datasets != null) {
                for (String dataset : datasets) {
                    datasetPaperMap.computeIfAbsent(dataset, k -> new ArrayList<>()).add(paper.getId());
                }
            }

            if (keywords != null) {
                paperKeywordsMap.put(paper.getId(), new HashSet<>(keywords));
            }

            Set<String> authorNames = getAuthorNames(paper);
            paperAuthorsMap.put(paper.getId(), authorNames);
        }

        List<SharedDatasetGroupDto> sharedDatasetGroups = new ArrayList<>();
        int suspectedCount = 0;
        Set<Long> suspectedPapers = new HashSet<>();

        for (String datasetName : targetDatasets) {
            List<Long> paperIds = datasetPaperMap.getOrDefault(datasetName, new ArrayList<>());
            if (paperIds.size() >= 2) {
                paperIds.add(paperId);

                double totalSimilarity = 0.0;
                int pairCount = 0;
                boolean isSuspiciousGroup = false;

                for (int i = 0; i < paperIds.size(); i++) {
                    for (int j = i + 1; j < paperIds.size(); j++) {
                        Long p1 = paperIds.get(i);
                        Long p2 = paperIds.get(j);

                        Set<String> kw1 = p1.equals(paperId) ? targetKeywords : paperKeywordsMap.getOrDefault(p1, Collections.emptySet());
                        Set<String> kw2 = p2.equals(paperId) ? targetKeywords : paperKeywordsMap.getOrDefault(p2, Collections.emptySet());
                        double keywordSim = calculateJaccardSimilarity(kw1, kw2);

                        Set<String> auth1 = p1.equals(paperId) ? targetAuthors : paperAuthorsMap.getOrDefault(p1, Collections.emptySet());
                        Set<String> auth2 = p2.equals(paperId) ? targetAuthors : paperAuthorsMap.getOrDefault(p2, Collections.emptySet());
                        double authorSim = calculateJaccardSimilarity(auth1, auth2);

                        totalSimilarity += keywordSim;
                        pairCount++;

                        if (keywordSim > 0.6 && authorSim > 0.5) {
                            isSuspiciousGroup = true;
                            suspectedPapers.add(p1);
                            suspectedPapers.add(p2);
                        }
                    }
                }

                double avgSimilarity = pairCount > 0 ? totalSimilarity / pairCount : 0.0;

                if (paperIds.size() >= 3 && avgSimilarity > 0.6) {
                    isSuspiciousGroup = true;
                    suspectedPapers.addAll(paperIds);
                }

                SharedDatasetGroupDto group = SharedDatasetGroupDto.builder()
                        .datasetName(datasetName)
                        .paperCount(paperIds.size())
                        .paperList(new ArrayList<>(paperIds))
                        .averageSimilarity(avgSimilarity)
                        .build();
                sharedDatasetGroups.add(group);

                if (isSuspiciousGroup) {
                    suspectedCount++;
                }
            }
        }

        boolean isSuspected = suspectedCount > 0 || !suspectedPapers.isEmpty();

        Map<String, Object> evidence = new HashMap<>();
        evidence.put("sharedDatasetCount", sharedDatasetGroups.size());
        evidence.put("suspiciousGroupCount", suspectedCount);
        evidence.put("suspectedPaperIds", new ArrayList<>(suspectedPapers));
        evidence.put("targetDatasets", new ArrayList<>(targetDatasets));
        evidence.put("targetKeywords", new ArrayList<>(targetKeywords));

        String description;
        if (isSuspected) {
            description = String.format("检测到疑似\"切香肠\"行为：发现 %d 个可疑数据集共享组，涉及 %d 篇论文存在高度关键词和作者重叠。",
                    suspectedCount, suspectedPapers.size());
        } else {
            description = "未检测到明显的\"切香肠\"行为。";
        }

        return SausageDetectionDto.builder()
                .isSuspected(isSuspected)
                .suspectedPaperCount(suspectedPapers.size())
                .sharedDatasetGroups(sharedDatasetGroups)
                .evidence(evidence)
                .description(description)
                .build();
    }

    private double calculateJaccardSimilarity(Set<String> set1, Set<String> set2) {
        if (set1.isEmpty() && set2.isEmpty()) {
            return 0.0;
        }
        Set<String> intersection = new HashSet<>(set1);
        intersection.retainAll(set2);
        Set<String> union = new HashSet<>(set1);
        union.addAll(set2);
        return union.isEmpty() ? 0.0 : (double) intersection.size() / union.size();
    }

    private List<String> getIntersection(Set<String> set1, Set<String> set2) {
        Set<String> intersection = new HashSet<>(set1);
        intersection.retainAll(set2);
        return new ArrayList<>(intersection);
    }

    private String determineSimilarityType(double datasetSim, double keywordSim, double referenceSim, double authorSim) {
        double maxSim = Math.max(Math.max(datasetSim, keywordSim), Math.max(referenceSim, authorSim));
        if (maxSim == datasetSim && datasetSim > 0.3) {
            return "DATA_OVERLAP";
        } else if (maxSim == keywordSim && keywordSim > 0.3) {
            return "TEXT_OVERLAP";
        } else if (maxSim == authorSim && authorSim > 0.3) {
            return "AUTHOR_OVERLAP";
        } else if (maxSim == referenceSim && referenceSim > 0.3) {
            return "REFERENCE_OVERLAP";
        }
        return "MULTI_DIMENSIONAL_OVERLAP";
    }

    private String determineRiskLevel(double similarity) {
        if (similarity < 0.3) {
            return "LOW";
        } else if (similarity < 0.6) {
            return "MEDIUM";
        } else if (similarity < 0.8) {
            return "HIGH";
        } else {
            return "CRITICAL";
        }
    }

    private Set<String> getDatasetNames(Paper paper) {
        if (paper.getDatasets() == null) {
            return Collections.emptySet();
        }
        return paper.getDatasets().stream()
                .map(Dataset::getName)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
    }

    private Set<String> getKeywordNames(Paper paper) {
        if (paper.getKeywords() == null) {
            return Collections.emptySet();
        }
        return paper.getKeywords().stream()
                .map(Keyword::getName)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
    }

    private Set<String> getReferenceTitles(Paper paper) {
        if (paper.getReferences() == null) {
            return Collections.emptySet();
        }
        return paper.getReferences().stream()
                .map(Reference::getTitle)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
    }

    private Set<String> getAuthorNames(Paper paper) {
        if (paper.getAuthors() == null) {
            return Collections.emptySet();
        }
        return paper.getAuthors().stream()
                .map(Author::getName)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
    }

    private Map<String, Object> buildEvidenceDetails(Paper paper, List<SimilarPaperDto> similarPapers,
                                                      SausageDetectionDto sausageDetection) {
        Map<String, Object> evidence = new HashMap<>();
        evidence.put("paperTitle", paper.getTitle());
        evidence.put("datasetCount", getDatasetNames(paper).size());
        evidence.put("keywordCount", getKeywordNames(paper).size());
        evidence.put("referenceCount", getReferenceTitles(paper).size());
        evidence.put("authorCount", getAuthorNames(paper).size());
        evidence.put("similarPaperCount", similarPapers.size());
        evidence.put("highRiskCount", similarPapers.stream()
                .filter(sp -> "HIGH".equals(sp.getRiskLevel()) || "CRITICAL".equals(sp.getRiskLevel()))
                .count());
        evidence.put("sausageSuspected", sausageDetection.getIsSuspected());
        evidence.put("detectionAlgorithm", "Jaccard相似度+多维度加权+聚类分析");
        evidence.put("weights", Map.of(
                "dataset", DATASET_WEIGHT,
                "keyword", KEYWORD_WEIGHT,
                "reference", REFERENCE_WEIGHT,
                "author", AUTHOR_WEIGHT
        ));
        return evidence;
    }

    private void saveDuplicateCheckReport(Paper paper, DuplicateCheckResult result,
                                          SausageDetectionDto sausageDetection,
                                          Map<String, Object> evidenceDetails) {
        try {
            DuplicateCheckReport report = DuplicateCheckReport.builder()
                    .paperId(paper.getId())
                    .paperTitle(paper.getTitle())
                    .checkTime(result.getCheckTime())
                    .overallRiskLevel(result.getOverallRiskLevel())
                    .overallSimilarity(result.getOverallSimilarity())
                    .similarPapersJson(objectMapper.writeValueAsString(result.getSimilarPapers()))
                    .sausageDetectionJson(objectMapper.writeValueAsString(sausageDetection))
                    .evidenceDetailsJson(objectMapper.writeValueAsString(evidenceDetails))
                    .reportStatus("PENDING")
                    .suspectedSausageCount(sausageDetection.getSuspectedPaperCount())
                    .build();
            duplicateCheckReportRepository.save(report);
        } catch (JsonProcessingException e) {
            log.error("序列化检测报告失败", e);
            throw new BusinessException("保存检测报告失败");
        }
    }
}
