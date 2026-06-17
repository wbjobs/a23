package com.research.backend.service.impl;

import com.research.backend.neo4j.entity.Paper;
import com.research.backend.neo4j.entity.Reviewer;
import com.research.backend.service.EmailService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;

@Service
@RequiredArgsConstructor
public class EmailServiceImpl implements EmailService {

    private final JavaMailSender javaMailSender;

    @Value("${spring.mail.username:noreply@research.edu}")
    private String senderEmail;

    @Override
    @Async
    public void sendReviewNotification(Reviewer reviewer, Paper paper) {
        String subject = "论文评审邀请 - " + paper.getTitle();
        String content = buildReviewNotificationHtml(reviewer, paper);
        sendEmail(reviewer.getEmail(), subject, content);
    }

    @Override
    @Async
    public void sendResultNotification(String authorEmail, Paper paper, String result) {
        String subject = "论文评审结果通知 - " + paper.getTitle();
        String content = buildResultNotificationHtml(paper, result);
        sendEmail(authorEmail, subject, content);
    }

    private String buildReviewNotificationHtml(Reviewer reviewer, Paper paper) {
        return "<!DOCTYPE html>" +
                "<html><head><meta charset='UTF-8'><title>评审邀请</title></head>" +
                "<body style='font-family: Arial, sans-serif; padding: 20px;'>" +
                "<div style='max-width: 600px; margin: 0 auto;'>" +
                "<h2 style='color: #2c3e50;'>尊敬的 " + reviewer.getName() + " 教授：</h2>" +
                "<p>您好！诚挚邀请您为以下论文进行评审：</p>" +
                "<div style='background: #f8f9fa; padding: 20px; border-radius: 8px; margin: 20px 0;'>" +
                "<h3 style='color: #34495e; margin-top: 0;'>" + paper.getTitle() + "</h3>" +
                "<p style='color: #666;'><strong>作者：</strong>" + (paper.getAuthorName() != null ? paper.getAuthorName() : "未知") + "</p>" +
                "<p style='color: #666;'><strong>摘要：</strong></p>" +
                "<p style='color: #555; line-height: 1.6;'>" +
                (paper.getAbstractText() != null && paper.getAbstractText().length() > 300 ?
                        paper.getAbstractText().substring(0, 300) + "..." :
                        (paper.getAbstractText() != null ? paper.getAbstractText() : "暂无摘要")) +
                "</p></div>" +
                "<p>如果您接受此评审任务，请登录系统完成评审。感谢您的支持！</p>" +
                "<p style='color: #888; font-size: 12px; margin-top: 30px;'>此邮件由系统自动发送，请勿直接回复。</p>" +
                "</div></body></html>";
    }

    private String buildResultNotificationHtml(Paper paper, String result) {
        String resultText = "ACCEPTED".equals(result) ? "录用" :
                "REJECTED".equals(result) ? "拒稿" : "需要修改后再审";
        String resultColor = "ACCEPTED".equals(result) ? "#27ae60" :
                "REJECTED".equals(result) ? "#e74c3c" : "#f39c12";

        return "<!DOCTYPE html>" +
                "<html><head><meta charset='UTF-8'><title>评审结果</title></head>" +
                "<body style='font-family: Arial, sans-serif; padding: 20px;'>" +
                "<div style='max-width: 600px; margin: 0 auto;'>" +
                "<h2 style='color: #2c3e50;'>尊敬的作者：</h2>" +
                "<p>您投稿的论文评审结果如下：</p>" +
                "<div style='background: #f8f9fa; padding: 20px; border-radius: 8px; margin: 20px 0;'>" +
                "<h3 style='color: #34495e; margin-top: 0;'>" + paper.getTitle() + "</h3>" +
                "<p style='font-size: 18px;'><strong>评审结果：</strong>" +
                "<span style='color: " + resultColor + "; font-weight: bold;'>" + resultText + "</span></p>" +
                "</div>" +
                "<p>请登录系统查看详细评审意见。</p>" +
                "<p style='color: #888; font-size: 12px; margin-top: 30px;'>此邮件由系统自动发送，请勿直接回复。</p>" +
                "</div></body></html>";
    }

    private void sendEmail(String to, String subject, String htmlContent) {
        try {
            MimeMessage message = javaMailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
            helper.setFrom(senderEmail);
            helper.setTo(to);
            helper.setSubject(subject);
            helper.setText(htmlContent, true);
            javaMailSender.send(message);
        } catch (MessagingException e) {
            throw new RuntimeException("邮件发送失败: " + e.getMessage());
        }
    }
}
