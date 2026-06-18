package com.research.backend.service;

import com.research.backend.neo4j.entity.Paper;
import com.research.backend.neo4j.entity.Reviewer;

public interface EmailService {
    void sendReviewNotification(Reviewer reviewer, Paper paper);
    void sendResultNotification(String authorEmail, Paper paper, String result);
    void sendReplyNotification(String reviewerEmail, String reviewerName, Paper paper, String authorName, String replyContent);
}
