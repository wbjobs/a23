package com.research.backend.service;

import com.research.backend.dto.ReplyThreadDto;
import com.research.backend.dto.ReviewReplyDto;
import com.research.backend.dto.ReviewReplyRequest;
import com.research.backend.dto.SuggestedReferenceDto;

import java.util.List;

public interface ReviewReplyService {

    ReviewReplyDto submitReply(ReviewReplyRequest request, Long authorId);

    List<ReplyThreadDto> getReplyThreads(Long reviewRecordId);

    List<ReviewReplyDto> getRepliesByThread(Long reviewRecordId, Integer threadNumber);

    List<SuggestedReferenceDto> suggestRelatedReferences(Long paperId, String content);

    ReviewReplyDto addReviewerResponse(Long replyId, Long reviewerId, String response);

    boolean markResolved(Long replyId, Long userId);

    List<ReviewReplyDto> getAuthorReplies(Long authorId, Long paperId);
}
