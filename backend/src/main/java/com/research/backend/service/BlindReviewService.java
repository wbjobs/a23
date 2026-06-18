package com.research.backend.service;

import com.research.backend.dto.BlindReviewConfigDto;
import com.research.backend.dto.PaperDesensitizedDto;
import com.research.backend.entity.ReviewRecord;

import java.util.List;

public interface BlindReviewService {

    PaperDesensitizedDto desensitizePaper(Long paperId, Long viewerId, String viewerRole);

    ReviewRecord desensitizeReviewRecord(ReviewRecord record, Long viewerId, String viewerRole);

    List<ReviewRecord> desensitizeReviewList(List<ReviewRecord> records, Long viewerId, String viewerRole);

    boolean toggleBlindReview(Long paperId, boolean enabled);

    void assignReviewerAlias(Long paperId, Long reviewerId);

    String desensitizeText(String text);

    BlindReviewConfigDto getGlobalConfig();

    BlindReviewConfigDto updateGlobalConfig(BlindReviewConfigDto configDto);
}
