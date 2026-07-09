package cn.example.aiagent.review;

import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Service
public class HumanReviewService {

    @Value("${ai.agent.human-review.threshold:0.8}")
    private double reviewThreshold;

    @Value("${ai.agent.human-review.enabled:true}")
    private boolean reviewEnabled;

    private final Map<String, ReviewRequest> reviewRequests = new ConcurrentHashMap<>();

    public ReviewRequest createReviewRequest(String content, String source) {
        ReviewRequest request = new ReviewRequest();
        request.setRequestId(UUID.randomUUID().toString());
        request.setContent(content);
        request.setSource(source);
        request.setCreatedAt(LocalDateTime.now());
        request.setStatus("pending");

        double riskScore = calculateRiskScore(content);
        request.setRiskScore(riskScore);
        request.setNeedsReview(riskScore >= reviewThreshold && reviewEnabled);

        reviewRequests.put(request.getRequestId(), request);
        log.info("创建审核请求: requestId={}, riskScore={}, needsReview={}", 
                request.getRequestId(), riskScore, request.isNeedsReview());

        return request;
    }

    private double calculateRiskScore(String content) {
        double score = 0.0;

        if (content.length() > 1000) {
            score += 0.2;
        }

        String[] sensitiveKeywords = {"敏感", "机密", "隐私", "密码", "token", "密钥"};
        for (String keyword : sensitiveKeywords) {
            if (content.contains(keyword)) {
                score += 0.1;
            }
        }

        String[] actionKeywords = {"删除", "修改", "执行", "发送", "上传"};
        for (String keyword : actionKeywords) {
            if (content.contains(keyword)) {
                score += 0.1;
            }
        }

        return Math.min(1.0, score);
    }

    public ReviewRequest getReviewRequest(String requestId) {
        return reviewRequests.get(requestId);
    }

    public List<ReviewRequest> getPendingRequests() {
        return reviewRequests.values().stream()
                .filter(r -> "pending".equals(r.getStatus()))
                .toList();
    }

    public ReviewRequest submitReview(String requestId, String reviewer, boolean approved, String comments) {
        ReviewRequest request = reviewRequests.get(requestId);
        if (request == null) {
            return null;
        }

        request.setStatus(approved ? "approved" : "rejected");
        request.setReviewer(reviewer);
        request.setReviewedAt(LocalDateTime.now());
        request.setComments(comments);

        log.info("审核完成: requestId={}, status={}, reviewer={}", 
                requestId, request.getStatus(), reviewer);

        return request;
    }

    public boolean isApproved(String requestId) {
        ReviewRequest request = reviewRequests.get(requestId);
        return request != null && "approved".equals(request.getStatus());
    }

    public boolean checkAndSubmit(String content, String source) {
        ReviewRequest request = createReviewRequest(content, source);
        
        if (!request.isNeedsReview()) {
            request.setStatus("auto-approved");
            return true;
        }

        return false;
    }

    @Data
    public static class ReviewRequest {
        private String requestId;
        private String content;
        private String source;
        private String status;
        private double riskScore;
        private boolean needsReview;
        private String reviewer;
        private String comments;
        private LocalDateTime createdAt;
        private LocalDateTime reviewedAt;
    }
}
