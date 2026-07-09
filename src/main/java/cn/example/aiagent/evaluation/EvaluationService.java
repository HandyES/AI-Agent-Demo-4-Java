package cn.example.aiagent.evaluation;

import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Service
public class EvaluationService {

    private final Map<String, EvaluationResult> evaluationResults = new ConcurrentHashMap<>();

    public EvaluationResult evaluate(TaskInput task) {
        log.info("开始评测任务: {}", task.getTaskId());

        EvaluationResult result = new EvaluationResult();
        result.setTaskId(task.getTaskId());
        result.setTaskDescription(task.getDescription());
        result.setTimestamp(LocalDateTime.now());

        result.setCompletionScore(evaluateCompletion(task));
        result.setEfficiencyMetrics(evaluateEfficiency(task));
        result.setQualityMetrics(evaluateQuality(task));
        result.setRobustnessScore(evaluateRobustness(task));

        double overallScore = calculateOverallScore(result);
        result.setOverallScore(overallScore);
        result.setGrade(mapScoreToGrade(overallScore));

        evaluationResults.put(task.getTaskId(), result);
        log.info("评测完成: taskId={}, overallScore={}, grade={}", 
                task.getTaskId(), overallScore, result.getGrade());

        return result;
    }

    private double evaluateCompletion(TaskInput task) {
        if (task.getExpectedOutput() == null || task.getExpectedOutput().isEmpty()) {
            return 0.5;
        }

        if (task.getActualOutput() == null || task.getActualOutput().isEmpty()) {
            return 0.0;
        }

        String expected = task.getExpectedOutput().toLowerCase();
        String actual = task.getActualOutput().toLowerCase();

        int matchingKeywords = 0;
        int totalKeywords = 0;

        for (String keyword : expected.split("\\s+")) {
            if (keyword.length() > 2) {
                totalKeywords++;
                if (actual.contains(keyword)) {
                    matchingKeywords++;
                }
            }
        }

        if (totalKeywords == 0) {
            return actual.length() > 0 ? 0.7 : 0.0;
        }

        return Math.min(1.0, (double) matchingKeywords / totalKeywords);
    }

    private EfficiencyMetrics evaluateEfficiency(TaskInput task) {
        EfficiencyMetrics metrics = new EfficiencyMetrics();

        if (task.getStartTime() != null && task.getEndTime() != null) {
            metrics.setExecutionTimeMs(Duration.between(task.getStartTime(), task.getEndTime()).toMillis());
        } else {
            metrics.setExecutionTimeMs(task.getExecutionTimeMs());
        }

        metrics.setStepCount(task.getStepCount());
        metrics.setInputTokens(task.getInputTokens());
        metrics.setOutputTokens(task.getOutputTokens());

        metrics.setEfficiencyScore(calculateEfficiencyScore(metrics));

        return metrics;
    }

    private double calculateEfficiencyScore(EfficiencyMetrics metrics) {
        double timeScore = 1.0;
        if (metrics.getExecutionTimeMs() > 0) {
            timeScore = Math.max(0.1, 1.0 - (metrics.getExecutionTimeMs() / 60000.0));
        }

        double stepScore = 1.0;
        if (metrics.getStepCount() > 0) {
            stepScore = Math.max(0.1, 1.0 - ((metrics.getStepCount() - 1) * 0.1));
        }

        double tokenScore = 1.0;
        long totalTokens = metrics.getInputTokens() + metrics.getOutputTokens();
        if (totalTokens > 10000) {
            tokenScore = Math.max(0.1, 1.0 - ((totalTokens - 10000) / 50000.0));
        }

        return (timeScore + stepScore + tokenScore) / 3.0;
    }

    private QualityMetrics evaluateQuality(TaskInput task) {
        QualityMetrics metrics = new QualityMetrics();

        if (task.getActualOutput() != null && !task.getActualOutput().isEmpty()) {
            metrics.setAccuracyScore(calculateAccuracy(task));
            metrics.setConsistencyScore(calculateConsistency(task));
            metrics.setNaturalnessScore(calculateNaturalness(task));
        } else {
            metrics.setAccuracyScore(0.0);
            metrics.setConsistencyScore(0.0);
            metrics.setNaturalnessScore(0.0);
        }

        metrics.setQualityScore((metrics.getAccuracyScore() + metrics.getConsistencyScore() + metrics.getNaturalnessScore()) / 3.0);

        return metrics;
    }

    private double calculateAccuracy(TaskInput task) {
        if (task.getExpectedOutput() == null) {
            return 0.5;
        }

        String expected = task.getExpectedOutput().toLowerCase();
        String actual = task.getActualOutput().toLowerCase();

        int commonChars = 0;
        Set<Character> expectedChars = new HashSet<>();
        for (char c : expected.toCharArray()) {
            expectedChars.add(c);
        }

        for (char c : actual.toCharArray()) {
            if (expectedChars.contains(c)) {
                commonChars++;
            }
        }

        return (double) commonChars / Math.max(expected.length(), 1);
    }

    private double calculateConsistency(TaskInput task) {
        if (task.getPreviousOutputs() == null || task.getPreviousOutputs().isEmpty()) {
            return 0.5;
        }

        String current = task.getActualOutput().toLowerCase();
        int consistentCount = 0;

        for (String previous : task.getPreviousOutputs()) {
            if (previous.toLowerCase().contains(current.substring(0, Math.min(10, current.length())))) {
                consistentCount++;
            }
        }

        return (double) consistentCount / task.getPreviousOutputs().size();
    }

    private double calculateNaturalness(TaskInput task) {
        if (task.getActualOutput() == null) {
            return 0.0;
        }

        String output = task.getActualOutput();
        int sentenceCount = output.split("[.!?。！？]").length;
        double avgSentenceLength = (double) output.length() / Math.max(sentenceCount, 1);

        if (avgSentenceLength < 5 || avgSentenceLength > 100) {
            return 0.3;
        } else if (avgSentenceLength < 10 || avgSentenceLength > 60) {
            return 0.6;
        }

        return 0.9;
    }

    private double evaluateRobustness(TaskInput task) {
        if (task.getErrorRate() != null) {
            return Math.max(0.0, 1.0 - task.getErrorRate());
        }

        if (task.getHandledEdgeCases() != null && task.getTotalEdgeCases() != null && task.getTotalEdgeCases() > 0) {
            return (double) task.getHandledEdgeCases() / task.getTotalEdgeCases();
        }

        if (task.getActualOutput() != null && !task.getActualOutput().isEmpty()) {
            return 0.7;
        }

        return 0.5;
    }

    private double calculateOverallScore(EvaluationResult result) {
        return (result.getCompletionScore() * 0.3 +
                result.getEfficiencyMetrics().getEfficiencyScore() * 0.2 +
                result.getQualityMetrics().getQualityScore() * 0.3 +
                result.getRobustnessScore() * 0.2);
    }

    private String mapScoreToGrade(double score) {
        if (score >= 0.9) return "A";
        if (score >= 0.8) return "B";
        if (score >= 0.7) return "C";
        if (score >= 0.6) return "D";
        return "F";
    }

    public EvaluationResult getEvaluationResult(String taskId) {
        return evaluationResults.get(taskId);
    }

    public List<EvaluationResult> getAllEvaluationResults() {
        return new ArrayList<>(evaluationResults.values());
    }

    public EvaluationSummary getEvaluationSummary() {
        EvaluationSummary summary = new EvaluationSummary();
        List<EvaluationResult> results = getAllEvaluationResults();

        summary.setTotalEvaluations(results.size());
        
        if (!results.isEmpty()) {
            double avgCompletion = results.stream().mapToDouble(EvaluationResult::getCompletionScore).average().orElse(0);
            double avgEfficiency = results.stream().mapToDouble(r -> r.getEfficiencyMetrics().getEfficiencyScore()).average().orElse(0);
            double avgQuality = results.stream().mapToDouble(r -> r.getQualityMetrics().getQualityScore()).average().orElse(0);
            double avgRobustness = results.stream().mapToDouble(EvaluationResult::getRobustnessScore).average().orElse(0);
            double avgOverall = results.stream().mapToDouble(EvaluationResult::getOverallScore).average().orElse(0);

            summary.setAverageCompletionScore(avgCompletion);
            summary.setAverageEfficiencyScore(avgEfficiency);
            summary.setAverageQualityScore(avgQuality);
            summary.setAverageRobustnessScore(avgRobustness);
            summary.setAverageOverallScore(avgOverall);

            Map<String, Long> gradeCounts = new HashMap<>();
            for (EvaluationResult r : results) {
                gradeCounts.merge(r.getGrade(), 1L, Long::sum);
            }
            summary.setGradeDistribution(gradeCounts);
        }

        summary.setTimestamp(LocalDateTime.now());
        return summary;
    }

    @Data
    public static class TaskInput {
        private String taskId;
        private String description;
        private String expectedOutput;
        private String actualOutput;
        private List<String> previousOutputs;
        private LocalDateTime startTime;
        private LocalDateTime endTime;
        private long executionTimeMs;
        private int stepCount;
        private long inputTokens;
        private long outputTokens;
        private Double errorRate;
        private Integer handledEdgeCases;
        private Integer totalEdgeCases;
    }

    @Data
    public static class EvaluationResult {
        private String taskId;
        private String taskDescription;
        private double completionScore;
        private EfficiencyMetrics efficiencyMetrics;
        private QualityMetrics qualityMetrics;
        private double robustnessScore;
        private double overallScore;
        private String grade;
        private LocalDateTime timestamp;
    }

    @Data
    public static class EfficiencyMetrics {
        private long executionTimeMs;
        private int stepCount;
        private long inputTokens;
        private long outputTokens;
        private double efficiencyScore;
    }

    @Data
    public static class QualityMetrics {
        private double accuracyScore;
        private double consistencyScore;
        private double naturalnessScore;
        private double qualityScore;
    }

    @Data
    public static class EvaluationSummary {
        private int totalEvaluations;
        private double averageCompletionScore;
        private double averageEfficiencyScore;
        private double averageQualityScore;
        private double averageRobustnessScore;
        private double averageOverallScore;
        private Map<String, Long> gradeDistribution;
        private LocalDateTime timestamp;
    }
}
