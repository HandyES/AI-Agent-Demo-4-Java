package cn.example.aiagent.observability;

import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

@Slf4j
@Service
public class ObservabilityService {

    @Autowired
    private ObservabilityConfig observabilityConfig;

    private final AtomicLong requestCounter = new AtomicLong(0);
    private final AtomicLong errorCounter = new AtomicLong(0);
    private final AtomicLong totalTokenUsage = new AtomicLong(0);
    
    private final Map<String, AtomicLong> requestCountsByType = new ConcurrentHashMap<>();
    private final Map<String, List<Long>> latencyRecordsByType = new ConcurrentHashMap<>();
    private final List<TraceRecord> traceRecords = Collections.synchronizedList(new ArrayList<>());

    public TraceRecord startTrace(String operation, String context) {
        if (!observabilityConfig.isTracing()) {
            return null;
        }

        TraceRecord trace = new TraceRecord();
        trace.setTraceId(UUID.randomUUID().toString());
        trace.setOperation(operation);
        trace.setContext(context);
        trace.setStartTime(LocalDateTime.now());
        trace.setStartNanos(System.nanoTime());

        traceRecords.add(trace);
        log.debug("开始追踪: traceId={}, operation={}", trace.getTraceId(), operation);

        return trace;
    }

    public void endTrace(TraceRecord trace, String result, boolean success) {
        if (trace == null || !observabilityConfig.isTracing()) {
            return;
        }

        trace.setEndTime(LocalDateTime.now());
        trace.setDurationNanos(System.nanoTime() - trace.getStartNanos());
        trace.setResult(result);
        trace.setSuccess(success);

        log.debug("结束追踪: traceId={}, duration={}ms, success={}", 
                trace.getTraceId(), trace.getDurationMs(), success);
    }

    public void logInfo(String message, String... tags) {
        if (observabilityConfig.isLogging()) {
            StringBuilder logMessage = new StringBuilder(message);
            if (tags.length > 0) {
                logMessage.append(" [");
                for (int i = 0; i < tags.length; i += 2) {
                    if (i > 0) logMessage.append(", ");
                    logMessage.append(tags[i]).append("=").append(tags[i + 1]);
                }
                logMessage.append("]");
            }
            log.info(logMessage.toString());
        }
    }

    public void logError(String message, Throwable exception, String... tags) {
        if (observabilityConfig.isLogging()) {
            errorCounter.incrementAndGet();
            StringBuilder logMessage = new StringBuilder(message);
            if (tags.length > 0) {
                logMessage.append(" [");
                for (int i = 0; i < tags.length; i += 2) {
                    if (i > 0) logMessage.append(", ");
                    logMessage.append(tags[i]).append("=").append(tags[i + 1]);
                }
                logMessage.append("]");
            }
            log.error(logMessage.toString(), exception);
        }
    }

    public void incrementRequestCount(String type) {
        if (!observabilityConfig.isMetrics()) {
            return;
        }

        requestCounter.incrementAndGet();
        requestCountsByType.computeIfAbsent(type, k -> new AtomicLong(0)).incrementAndGet();
    }

    public void recordLatency(String type, long durationMs) {
        if (!observabilityConfig.isMetrics()) {
            return;
        }

        latencyRecordsByType.computeIfAbsent(type, k -> Collections.synchronizedList(new ArrayList<>()))
                .add(durationMs);
    }

    public void recordTokenUsage(long tokens) {
        if (!observabilityConfig.isMetrics()) {
            return;
        }
        totalTokenUsage.addAndGet(tokens);
    }

    public MetricsSummary getMetrics() {
        MetricsSummary summary = new MetricsSummary();
        summary.setTotalRequests(requestCounter.get());
        summary.setTotalErrors(errorCounter.get());
        summary.setTotalTokenUsage(totalTokenUsage.get());
        summary.setRequestCountsByType(new HashMap<>());
        summary.setLatencyStatsByType(new HashMap<>());

        requestCountsByType.forEach((type, count) -> 
                summary.getRequestCountsByType().put(type, count.get()));

        latencyRecordsByType.forEach((type, latencies) -> {
            if (!latencies.isEmpty()) {
                LatencyStats stats = new LatencyStats();
                stats.setCount(latencies.size());
                stats.setMin(latencies.stream().mapToLong(Long::longValue).min().orElse(0));
                stats.setMax(latencies.stream().mapToLong(Long::longValue).max().orElse(0));
                stats.setAvg(latencies.stream().mapToLong(Long::longValue).average().orElse(0));
                summary.getLatencyStatsByType().put(type, stats);
            }
        });

        return summary;
    }

    public List<TraceRecord> getRecentTraces(int limit) {
        List<TraceRecord> recent = new ArrayList<>(traceRecords);
        recent.sort((a, b) -> b.getStartTime().compareTo(a.getStartTime()));
        return recent.subList(0, Math.min(limit, recent.size()));
    }

    @Data
    public static class TraceRecord {
        private String traceId;
        private String operation;
        private String context;
        private LocalDateTime startTime;
        private LocalDateTime endTime;
        private long startNanos;
        private long durationNanos;
        private String result;
        private boolean success;

        public long getDurationMs() {
            return durationNanos / 1_000_000;
        }
    }

    @Data
    public static class MetricsSummary {
        private long totalRequests;
        private long totalErrors;
        private long totalTokenUsage;
        private Map<String, Long> requestCountsByType;
        private Map<String, LatencyStats> latencyStatsByType;
        private LocalDateTime timestamp = LocalDateTime.now();
    }

    @Data
    public static class LatencyStats {
        private long count;
        private long min;
        private long max;
        private double avg;
    }
}
