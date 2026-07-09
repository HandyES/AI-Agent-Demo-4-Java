package cn.example.aiagent.skill;

import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Service
public class SkillService {

    private final Map<String, Skill> registeredSkills = new ConcurrentHashMap<>();

    public SkillService() {
        registerBuiltInSkills();
    }

    private void registerBuiltInSkills() {
        Skill weatherSkill = new Skill();
        weatherSkill.setName("weather");
        weatherSkill.setDescription("天气查询技能");
        weatherSkill.setCategory("实用工具");
        weatherSkill.setVersion("1.0");
        registeredSkills.put("weather", weatherSkill);

        Skill calculatorSkill = new Skill();
        calculatorSkill.setName("calculator");
        calculatorSkill.setDescription("计算器技能");
        calculatorSkill.setCategory("实用工具");
        calculatorSkill.setVersion("1.0");
        registeredSkills.put("calculator", calculatorSkill);

        Skill translationSkill = new Skill();
        translationSkill.setName("translation");
        translationSkill.setDescription("翻译技能");
        translationSkill.setCategory("语言处理");
        translationSkill.setVersion("1.0");
        registeredSkills.put("translation", translationSkill);

        Skill summarizationSkill = new Skill();
        summarizationSkill.setName("summarization");
        summarizationSkill.setDescription("文本摘要技能");
        summarizationSkill.setCategory("语言处理");
        summarizationSkill.setVersion("1.0");
        registeredSkills.put("summarization", summarizationSkill);
    }

    public void registerSkill(Skill skill) {
        registeredSkills.put(skill.getName(), skill);
        log.info("注册技能: {}", skill.getName());
    }

    public List<Skill> listSkills() {
        return new ArrayList<>(registeredSkills.values());
    }

    public Skill getSkill(String name) {
        return registeredSkills.get(name);
    }

    public SkillExecutionResult executeSkill(String skillName, Map<String, Object> parameters) {
        log.info("执行技能: {}, 参数: {}", skillName, parameters);

        SkillExecutionResult result = new SkillExecutionResult();
        result.setSkillName(skillName);
        result.setParameters(parameters);
        result.setTimestamp(LocalDateTime.now());

        Skill skill = registeredSkills.get(skillName);
        if (skill == null) {
            result.setSuccess(false);
            result.setError("技能不存在: " + skillName);
            return result;
        }

        try {
            String output = executeBuiltInSkill(skillName, parameters);
            result.setSuccess(true);
            result.setOutput(output);
        } catch (Exception e) {
            result.setSuccess(false);
            result.setError("执行失败: " + e.getMessage());
            log.error("技能执行失败", e);
        }

        return result;
    }

    private String executeBuiltInSkill(String skillName, Map<String, Object> parameters) {
        return switch (skillName) {
            case "weather" -> {
                String city = (String) parameters.getOrDefault("city", "北京");
                yield String.format("%s的天气：晴，温度25-30度，微风。", city);
            }
            case "calculator" -> {
                String expression = (String) parameters.get("expression");
                try {
                    double calcResult = new javax.script.ScriptEngineManager()
                            .getEngineByName("JavaScript")
                            .eval(expression) instanceof Number ?
                            ((Number) new javax.script.ScriptEngineManager()
                                    .getEngineByName("JavaScript")
                                    .eval(expression)).doubleValue() : 0;
                    yield "计算结果：" + calcResult;
                } catch (Exception e) {
                    yield "计算失败：" + e.getMessage();
                }
            }
            case "translation" -> {
                String text = (String) parameters.get("text");
                String targetLang = (String) parameters.getOrDefault("target", "中文");
                yield String.format("翻译结果（%s）：这是'%s'的翻译。", targetLang, text);
            }
            case "summarization" -> {
                String text = (String) parameters.get("text");
                yield "摘要：" + text.substring(0, Math.min(100, text.length())) + "...";
            }
            default -> "未知技能";
        };
    }

    @Data
    public static class Skill {
        private String name;
        private String description;
        private String category;
        private String version;
        private List<String> tags = new ArrayList<>();
    }

    @Data
    public static class SkillExecutionResult {
        private String skillName;
        private Map<String, Object> parameters;
        private String output;
        private boolean success;
        private String error;
        private LocalDateTime timestamp;
    }
}
