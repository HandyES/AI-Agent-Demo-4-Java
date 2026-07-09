package cn.example.aiagent.mcp;

import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Service
public class McpService {

    private final Map<String, McpTool> registeredTools = new ConcurrentHashMap<>();

    public McpService() {
        registerBuiltInTools();
    }

    private void registerBuiltInTools() {
        McpTool weatherTool = new McpTool();
        weatherTool.setName("get_weather");
        weatherTool.setDescription("获取指定城市的天气信息");
        weatherTool.setParameters(List.of(
                createParameter("city", "string", "城市名称", true),
                createParameter("date", "string", "日期(可选，格式YYYY-MM-DD)", false)
        ));
        registeredTools.put("get_weather", weatherTool);

        McpTool calculatorTool = new McpTool();
        calculatorTool.setName("calculate");
        calculatorTool.setDescription("执行数学计算");
        calculatorTool.setParameters(List.of(
                createParameter("expression", "string", "数学表达式", true)
        ));
        registeredTools.put("calculate", calculatorTool);

        McpTool searchTool = new McpTool();
        searchTool.setName("web_search");
        searchTool.setDescription("执行网络搜索");
        searchTool.setParameters(List.of(
                createParameter("query", "string", "搜索关键词", true)
        ));
        registeredTools.put("web_search", searchTool);
    }

    private McpParameter createParameter(String name, String type, String description, boolean required) {
        McpParameter param = new McpParameter();
        param.setName(name);
        param.setType(type);
        param.setDescription(description);
        param.setRequired(required);
        return param;
    }

    public void registerTool(McpTool tool) {
        registeredTools.put(tool.getName(), tool);
        log.info("注册MCP工具: {}", tool.getName());
    }

    public List<McpTool> listTools() {
        return new ArrayList<>(registeredTools.values());
    }

    public McpTool getTool(String name) {
        return registeredTools.get(name);
    }

    public McpExecutionResult executeTool(String toolName, Map<String, Object> parameters) {
        log.info("执行MCP工具: {}, 参数: {}", toolName, parameters);

        McpExecutionResult result = new McpExecutionResult();
        result.setToolName(toolName);
        result.setParameters(parameters);
        result.setTimestamp(LocalDateTime.now());

        McpTool tool = registeredTools.get(toolName);
        if (tool == null) {
            result.setSuccess(false);
            result.setError("工具不存在: " + toolName);
            return result;
        }

        if (!validateParameters(tool, parameters)) {
            result.setSuccess(false);
            result.setError("参数验证失败");
            return result;
        }

        try {
            String output = executeBuiltInTool(toolName, parameters);
            result.setSuccess(true);
            result.setOutput(output);
        } catch (Exception e) {
            result.setSuccess(false);
            result.setError("执行失败: " + e.getMessage());
            log.error("MCP工具执行失败", e);
        }

        return result;
    }

    private boolean validateParameters(McpTool tool, Map<String, Object> parameters) {
        for (McpParameter param : tool.getParameters()) {
            if (param.isRequired() && !parameters.containsKey(param.getName())) {
                return false;
            }
        }
        return true;
    }

    private String executeBuiltInTool(String toolName, Map<String, Object> parameters) {
        return switch (toolName) {
            case "get_weather" -> {
                String city = (String) parameters.get("city");
                String date = parameters.containsKey("date") ? (String) parameters.get("date") : "今天";
                yield String.format("%s%s的天气：晴，温度25-30度，微风。", city, date);
            }
            case "calculate" -> {
                String expression = (String) parameters.get("expression");
                try {
                    double result = evaluateExpression(expression);
                    yield "计算结果：" + result;
                } catch (Exception e) {
                    yield "计算失败：" + e.getMessage();
                }
            }
            case "web_search" -> {
                String query = (String) parameters.get("query");
                yield String.format("搜索结果：关于'%s'的相关信息已找到，包含多个相关网页和资源。", query);
            }
            default -> "未知工具";
        };
    }

    private double evaluateExpression(String expression) {
        expression = expression.replaceAll("[^0-9+\\-*/().]", "");
        try {
            return new javax.script.ScriptEngineManager()
                    .getEngineByName("JavaScript")
                    .eval(expression) instanceof Number ?
                    ((Number) new javax.script.ScriptEngineManager()
                            .getEngineByName("JavaScript")
                            .eval(expression)).doubleValue() : 0;
        } catch (Exception e) {
            return simpleCalculate(expression);
        }
    }

    private double simpleCalculate(String expression) {
        try {
            expression = expression.replace(" ", "");
            if (expression.contains("+")) {
                String[] parts = expression.split("\\+");
                return Double.parseDouble(parts[0]) + Double.parseDouble(parts[1]);
            } else if (expression.contains("-")) {
                String[] parts = expression.split("-");
                return Double.parseDouble(parts[0]) - Double.parseDouble(parts[1]);
            } else if (expression.contains("*")) {
                String[] parts = expression.split("\\*");
                return Double.parseDouble(parts[0]) * Double.parseDouble(parts[1]);
            } else if (expression.contains("/")) {
                String[] parts = expression.split("/");
                return Double.parseDouble(parts[0]) / Double.parseDouble(parts[1]);
            }
            return Double.parseDouble(expression);
        } catch (Exception e) {
            return 0;
        }
    }

    @Data
    public static class McpExecutionResult {
        private String toolName;
        private Map<String, Object> parameters;
        private String output;
        private boolean success;
        private String error;
        private LocalDateTime timestamp;
    }
}
