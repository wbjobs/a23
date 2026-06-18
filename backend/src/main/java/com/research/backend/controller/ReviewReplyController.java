package com.research.backend.controller;

import com.research.backend.common.Result;
import com.research.backend.dto.ReplyThreadDto;
import com.research.backend.dto.ReviewReplyDto;
import com.research.backend.dto.ReviewReplyRequest;
import com.research.backend.dto.SuggestedReferenceDto;
import com.research.backend.service.ReviewReplyService;
import com.research.backend.util.JwtUtil;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/reply")
@RequiredArgsConstructor
public class ReviewReplyController {

    private final ReviewReplyService reviewReplyService;
    private final JwtUtil jwtUtil;

    @PostMapping("/submit")
    @PreAuthorize("hasAnyRole('AUTHOR', 'ADMIN')")
    public Result<ReviewReplyDto> submitReply(
            @Valid @RequestBody ReviewReplyRequest request,
            @RequestHeader("Authorization") String authHeader) {
        Long authorId = getUserIdFromHeader(authHeader);
        ReviewReplyDto reply = reviewReplyService.submitReply(request, authorId);
        return Result.success("回复提交成功", reply);
    }

    @GetMapping("/review/{reviewRecordId}/threads")
    public Result<List<ReplyThreadDto>> getReplyThreads(
            @PathVariable Long reviewRecordId) {
        List<ReplyThreadDto> threads = reviewReplyService.getReplyThreads(reviewRecordId);
        return Result.success(threads);
    }

    @GetMapping("/review/{reviewRecordId}/thread/{threadNumber}")
    public Result<List<ReviewReplyDto>> getRepliesByThread(
            @PathVariable Long reviewRecordId,
            @PathVariable Integer threadNumber) {
        List<ReviewReplyDto> replies = reviewReplyService.getRepliesByThread(reviewRecordId, threadNumber);
        return Result.success(replies);
    }

    @GetMapping("/suggest")
    public Result<List<SuggestedReferenceDto>> getSuggestedReferences(
            @RequestParam Long paperId,
            @RequestParam String content) {
        List<SuggestedReferenceDto> references = reviewReplyService.suggestRelatedReferences(paperId, content);
        return Result.success(references);
    }

    @PutMapping("/{replyId}/response")
    @PreAuthorize("hasAnyRole('REVIEWER', 'ADMIN')")
    public Result<ReviewReplyDto> addReviewerResponse(
            @PathVariable Long replyId,
            @RequestBody Map<String, String> body,
            @RequestHeader("Authorization") String authHeader) {
        Long reviewerId = getUserIdFromHeader(authHeader);
        String response = body.get("response");
        ReviewReplyDto reply = reviewReplyService.addReviewerResponse(replyId, reviewerId, response);
        return Result.success("回应已提交", reply);
    }

    @PutMapping("/{replyId}/resolve")
    @PreAuthorize("hasAnyRole('AUTHOR', 'REVIEWER', 'ADMIN')")
    public Result<Boolean> markResolved(
            @PathVariable Long replyId,
            @RequestHeader("Authorization") String authHeader) {
        Long userId = getUserIdFromHeader(authHeader);
        boolean result = reviewReplyService.markResolved(replyId, userId);
        return Result.success("已标记为已解决", result);
    }

    @GetMapping("/author/list")
    @PreAuthorize("hasAnyRole('AUTHOR', 'ADMIN')")
    public Result<List<ReviewReplyDto>> getAuthorReplies(
            @RequestParam(required = false) Long paperId,
            @RequestHeader("Authorization") String authHeader) {
        Long authorId = getUserIdFromHeader(authHeader);
        List<ReviewReplyDto> replies = reviewReplyService.getAuthorReplies(authorId, paperId);
        return Result.success(replies);
    }

    private Long getUserIdFromHeader(String authHeader) {
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            String token = authHeader.substring(7);
            return jwtUtil.getUserIdFromToken(token);
        }
        throw new RuntimeException("未授权");
    }
}
