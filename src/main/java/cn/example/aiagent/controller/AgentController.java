package cn.example.aiagent.controller;

import cn.example.aiagent.computeruse.ComputerUseService;
import cn.example.aiagent.evaluation.EvaluationService;
import cn.example.aiagent.llm.LlmService;
import cn.example.aiagent.mcp.McpService;
import cn.example.aiagent.multimodal.MultimodalService;
import cn.example.aiagent.observability.ObservabilityService;
import cn.example.aiagent.rag.RagService;
import cn.example.aiagent.react.ReActEngine;
import cn.example.aiagent.review.HumanReviewService;
import cn.example.aiagent.security.SecurityService;
import cn.example.aiagent.skill.SkillService;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/agent")
public class AgentController {

    @Autowired
    private LlmService llmService;

    @Autowired
    private ReActEngine reActEngine;

    @Autowired
    private RagService ragService;

    @Autowired
    private SecurityService securityService;

    @Autowired
    private ObservabilityService observabilityService;

    @Autowired
    private EvaluationService evaluationService;

    @Autowired
    private ComputerUseService computerUseService;

    @Autowired
    private McpService mcpService;

    @Autowired
    private SkillService skillService;

    @Autowired
    private HumanReviewService humanReviewService;

    @Autowired
    private MultimodalService multimodalService;

    @PostMapping("/chat")
    public ResponseEntity<?> chat(@RequestBody ChatRequest request) {
        log.info("接收到聊天请求: {}", request.getMessage());

        var securityResult = securityService.checkInput(request.getMessage());
        if (!securityResult.isPassed()) {
            return ResponseEntity.badRequest().build();
        }

        var response = llmService.generateResponse(request.getMessage());
        return ResponseEntity.ok(response);
    }

    @PostMapping("/react")
    public ResponseEntity<ReActEngine.ReActResult> react(@RequestBody ChatRequest request) {
        log.info("接收到ReAct推理请求: {}", request.getMessage());

        var securityResult = securityService.checkInput(request.getMessage());
        if (!securityResult.isPassed()) {
            return ResponseEntity.badRequest().build();
        }

        var result = reActEngine.execute(request.getMessage());
        return ResponseEntity.ok(result);
    }

    @PostMapping("/rag")
    public ResponseEntity<RagService.RagResult> rag(@RequestBody RagRequest request) {
        log.info("接收到RAG查询请求: {}, mode={}", request.getQuestion(), request.getMode());

        var securityResult = securityService.checkInput(request.getQuestion());
        if (!securityResult.isPassed()) {
            return ResponseEntity.badRequest().build();
        }

        var result = ragService.query(request.getQuestion(), request.getMode());
        return ResponseEntity.ok(result);
    }

    @PostMapping("/security/check")
    public ResponseEntity<SecurityService.SecurityCheckResult> checkSecurity(@RequestBody ChatRequest request) {
        log.info("接收到安全检查请求");
        var result = securityService.checkInput(request.getMessage());
        return ResponseEntity.ok(result);
    }

    @GetMapping("/observability/metrics")
    public ResponseEntity<ObservabilityService.MetricsSummary> getMetrics() {
        log.info("接收到获取指标请求");
        var metrics = observabilityService.getMetrics();
        return ResponseEntity.ok(metrics);
    }

    @GetMapping("/observability/traces")
    public ResponseEntity<?> getTraces(@RequestParam(defaultValue = "10") int limit) {
        log.info("接收到获取追踪记录请求");
        var traces = observabilityService.getRecentTraces(limit);
        return ResponseEntity.ok(traces);
    }

    @PostMapping("/evaluation")
    public ResponseEntity<EvaluationService.EvaluationResult> evaluate(@RequestBody EvaluationService.TaskInput task) {
        log.info("接收到评测请求: {}", task.getTaskId());
        var result = evaluationService.evaluate(task);
        return ResponseEntity.ok(result);
    }

    @GetMapping("/evaluation/summary")
    public ResponseEntity<EvaluationService.EvaluationSummary> getEvaluationSummary() {
        log.info("接收到获取评测汇总请求");
        var summary = evaluationService.getEvaluationSummary();
        return ResponseEntity.ok(summary);
    }

    @PostMapping("/computer-use")
    public ResponseEntity<ComputerUseService.ComputerUseResult> computerUse(
            @RequestBody ComputerUseService.ComputerUseCommand command) {
        log.info("接收到Computer Use请求: {}", command.getAction());

        var securityResult = securityService.checkInput(command.toString());
        if (!securityResult.isPassed()) {
            return ResponseEntity.badRequest().build();
        }

        var result = computerUseService.executeCommand(command);
        return ResponseEntity.ok(result);
    }

    @PostMapping("/mcp/execute")
    public ResponseEntity<McpService.McpExecutionResult> executeMcp(
            @RequestBody McpRequest request) {
        log.info("接收到MCP执行请求: {}", request.getToolName());
        var result = mcpService.executeTool(request.getToolName(), request.getParameters());
        return ResponseEntity.ok(result);
    }

    @GetMapping("/mcp/tools")
    public ResponseEntity<?> listMcpTools() {
        log.info("接收到获取MCP工具列表请求");
        var tools = mcpService.listTools();
        return ResponseEntity.ok(tools);
    }

    @PostMapping("/skill/execute")
    public ResponseEntity<SkillService.SkillExecutionResult> executeSkill(
            @RequestBody SkillRequest request) {
        log.info("接收到技能执行请求: {}", request.getSkillName());
        var result = skillService.executeSkill(request.getSkillName(), request.getParameters());
        return ResponseEntity.ok(result);
    }

    @GetMapping("/skill/list")
    public ResponseEntity<?> listSkills() {
        log.info("接收到获取技能列表请求");
        var skills = skillService.listSkills();
        return ResponseEntity.ok(skills);
    }

    @PostMapping("/review/create")
    public ResponseEntity<HumanReviewService.ReviewRequest> createReview(
            @RequestBody ReviewRequest request) {
        log.info("接收到创建审核请求");
        var result = humanReviewService.createReviewRequest(request.getContent(), request.getSource());
        return ResponseEntity.ok(result);
    }

    @PostMapping("/review/submit")
    public ResponseEntity<HumanReviewService.ReviewRequest> submitReview(
            @RequestBody ReviewSubmitRequest request) {
        log.info("接收到提交审核请求: {}", request.getRequestId());
        var result = humanReviewService.submitReview(
                request.getRequestId(), request.getReviewer(), 
                request.isApproved(), request.getComments());
        if (result == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(result);
    }

    @GetMapping("/review/pending")
    public ResponseEntity<?> getPendingReviews() {
        log.info("接收到获取待审核列表请求");
        var requests = humanReviewService.getPendingRequests();
        return ResponseEntity.ok(requests);
    }

    @PostMapping("/multimodal/process")
    public ResponseEntity<MultimodalService.MultimodalResult> processMultimodal(
            @RequestBody MultimodalService.MultimodalInput input) {
        log.info("接收到多模态处理请求: {}", input.getType());
        var result = multimodalService.process(input);
        return ResponseEntity.ok(result);
    }

    @GetMapping("/llm/provider")
    public ResponseEntity<Map<String, String>> getLlmProvider() {
        log.info("接收到获取LLM提供商请求");
        return ResponseEntity.ok(Map.of("provider", llmService.getCurrentProvider()));
    }

    @PostMapping("/llm/provider")
    public ResponseEntity<Map<String, String>> setLlmProvider(@RequestBody Map<String, String> request) {
        String provider = request.get("provider");
        log.info("接收到设置LLM提供商请求: {}", provider);
        llmService.setProvider(provider);
        return ResponseEntity.ok(Map.of("provider", llmService.getCurrentProvider()));
    }

    @Data
    public static class ChatRequest {
        private String message;
    }

    @Data
    public static class RagRequest {
        private String question;
        private String mode = "basic";
    }

    @Data
    public static class McpRequest {
        private String toolName;
        private Map<String, Object> parameters;
    }

    @Data
    public static class SkillRequest {
        private String skillName;
        private Map<String, Object> parameters;
    }

    @Data
    public static class ReviewRequest {
        private String content;
        private String source;
    }

    @Data
    public static class ReviewSubmitRequest {
        private String requestId;
        private String reviewer;
        private boolean approved;
        private String comments;
    }
}
