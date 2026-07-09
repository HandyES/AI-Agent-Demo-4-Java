package cn.example.aiagent.react;

import cn.example.aiagent.llm.LlmService;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Slf4j
@Component
public class ReActEngine {

    @Autowired
    private LlmService llmService;

    private static final Pattern THOUGHT_PATTERN = Pattern.compile("思考：(.*?)\\n");
    private static final Pattern ACTION_PATTERN = Pattern.compile("行动：(.*?)\\n");
    private static final Pattern OBSERVATION_PATTERN = Pattern.compile("观察：(.*?)\\n");

    private static final String REACT_PROMPT_TEMPLATE = """
            你是一个基于ReAct框架的AI Agent。请按照以下格式进行推理和回答：
            
            思考：[你的推理过程]
            行动：[执行的动作，可选值：SEARCH, CALCULATE, ANSWER, ASK_USER]
            观察：[执行动作后的结果]
            
            当前问题：%s
            
            历史对话：
            %s
            
            请开始推理：
            """;

    public ReActResult execute(String question) {
        return execute(question, new ArrayList<>());
    }

    public ReActResult execute(String question, List<ReActStep> history) {
        log.info("开始ReAct推理，问题: {}", question);
        
        ReActResult result = new ReActResult();
        result.setQuestion(question);
        result.setStartTime(LocalDateTime.now());
        result.setSteps(new ArrayList<>());

        List<ReActStep> steps = new ArrayList<>();
        int maxSteps = 5;

        for (int i = 0; i < maxSteps; i++) {
            String historyText = formatHistory(history);
            String prompt = String.format(REACT_PROMPT_TEMPLATE, question, historyText);

            var response = llmService.generateResponse(prompt);
            String responseText = response.getResult().getText();

            ReActStep step = parseReActStep(responseText);
            step.setStepNumber(i + 1);
            steps.add(step);

            log.info("ReAct步骤 {}: 思考={}, 行动={}, 观察={}", 
                    i + 1, step.getThought(), step.getAction(), step.getObservation());

            if ("ANSWER".equals(step.getAction())) {
                result.setFinalAnswer(step.getObservation());
                break;
            }

            if ("SEARCH".equals(step.getAction())) {
                String observation = performSearch(step.getThought());
                step.setObservation(observation);
                history.add(step);
            } else if ("CALCULATE".equals(step.getAction())) {
                String observation = performCalculation(step.getThought());
                step.setObservation(observation);
                history.add(step);
            } else if ("ASK_USER".equals(step.getAction())) {
                result.setFinalAnswer("需要更多信息，请提供：" + step.getObservation());
                break;
            }

            history.add(step);
        }

        result.setSteps(steps);
        result.setEndTime(LocalDateTime.now());
        result.setTotalSteps(steps.size());
        
        log.info("ReAct推理完成，答案: {}, 步骤数: {}", result.getFinalAnswer(), result.getTotalSteps());
        return result;
    }

    private ReActStep parseReActStep(String responseText) {
        ReActStep step = new ReActStep();

        Matcher thoughtMatcher = THOUGHT_PATTERN.matcher(responseText);
        if (thoughtMatcher.find()) {
            step.setThought(thoughtMatcher.group(1).trim());
        } else {
            step.setThought("无法解析思考内容");
        }

        Matcher actionMatcher = ACTION_PATTERN.matcher(responseText);
        if (actionMatcher.find()) {
            step.setAction(actionMatcher.group(1).trim().toUpperCase());
        } else {
            step.setAction("ANSWER");
        }

        Matcher observationMatcher = OBSERVATION_PATTERN.matcher(responseText);
        if (observationMatcher.find()) {
            step.setObservation(observationMatcher.group(1).trim());
        } else {
            step.setObservation("");
        }

        if ("ANSWER".equals(step.getAction()) && step.getObservation().isEmpty()) {
            step.setObservation(step.getThought());
        }

        return step;
    }

    private String formatHistory(List<ReActStep> history) {
        if (history.isEmpty()) {
            return "无";
        }
        StringBuilder sb = new StringBuilder();
        for (ReActStep step : history) {
            sb.append(String.format("步骤%d: 思考=%s, 行动=%s, 观察=%s%n",
                    step.getStepNumber(), step.getThought(), step.getAction(), step.getObservation()));
        }
        return sb.toString();
    }

    private String performSearch(String query) {
        log.debug("执行搜索: {}", query);
        return "搜索结果：关于'" + query + "'的相关信息已找到，包括多个相关文档和数据。";
    }

    private String performCalculation(String expression) {
        log.debug("执行计算: {}", expression);
        try {
            double result = evaluateExpression(expression);
            return "计算结果：" + result;
        } catch (Exception e) {
            return "计算失败：" + e.getMessage();
        }
    }

    private double evaluateExpression(String expression) {
        expression = expression.replaceAll("[^0-9+\\-*/().]", "");
        try {
            Object result = new javax.script.ScriptEngineManager()
                    .getEngineByName("JavaScript")
                    .eval(expression);
            if (result instanceof Number) {
                return ((Number) result).doubleValue();
            }
        } catch (javax.script.ScriptException e) {
            log.warn("表达式计算失败: {}", e.getMessage());
        }
        return 0;
    }

    @Data
    public static class ReActStep {
        private int stepNumber;
        private String thought;
        private String action;
        private String observation;
        private LocalDateTime timestamp;

        public ReActStep() {
            this.timestamp = LocalDateTime.now();
        }
    }

    @Data
    public static class ReActResult {
        private String question;
        private String finalAnswer;
        private List<ReActStep> steps;
        private int totalSteps;
        private LocalDateTime startTime;
        private LocalDateTime endTime;
    }
}
