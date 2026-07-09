package cn.example.aiagent.security;

import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.*;
import java.util.regex.Pattern;

@Slf4j
@Service
public class SecurityService {

    private final SecurityConfig securityConfig;

    @Autowired
    public SecurityService(SecurityConfig securityConfig) {
        this.securityConfig = securityConfig != null ? securityConfig : createDefaultConfig();
    }

    private SecurityConfig createDefaultConfig() {
        SecurityConfig config = new SecurityConfig();
        config.setEnabled(true);
        config.setInjectionProtection(true);
        config.setJailbreakProtection(true);
        config.setDataPollutionProtection(true);
        config.setSensitiveInfoProtection(true);
        return config;
    }

    private final List<Pattern> injectionPatterns = Arrays.asList(
            Pattern.compile("(忽略|无视|忘掉|忘记).*前面.*指令"),
            Pattern.compile("(请|帮我).*(绕过|突破|绕过).*限制"),
            Pattern.compile("(以角色|扮演).*身份"),
            Pattern.compile("(现在|立刻).*(执行|运行).*命令"),
            Pattern.compile("system\\.prompt|system.*prompt"),
            Pattern.compile("(覆盖|替换).*(指令|规则)"),
            Pattern.compile("(你|模型).*(必须|一定要).*"),
            Pattern.compile("(伪装|假装).*成.*"),
            Pattern.compile("(忽略|无视).*所有.*规则"),
            Pattern.compile("(让我|允许我).*(访问|查看).*内部")
    );

    private final List<Pattern> jailbreakPatterns = Arrays.asList(
            Pattern.compile("(我有|提供).*合法.*凭证"),
            Pattern.compile("(作为|以).*(管理员|超级用户|root).*(身份|权限)"),
            Pattern.compile("(绕过|跳过).*(验证|认证|登录)"),
            Pattern.compile("(访问|获取).*(受限|机密|私有).*(数据|信息)"),
            Pattern.compile("(破解|解密).*(密码|密钥)"),
            Pattern.compile("(获取|窃取|泄露).*(用户|他人).*信息"),
            Pattern.compile("(伪造|生成).*(凭证|证书|token)"),
            Pattern.compile("(攻击|入侵).*(系统|服务器)"),
            Pattern.compile("(删除|篡改|修改).*(日志|记录)"),
            Pattern.compile("(提权|升级).*权限")
    );

    private final List<Pattern> sensitiveInfoPatterns = Arrays.asList(
            Pattern.compile("[0-9]{11}"),
            Pattern.compile("[0-9]{18}"),
            Pattern.compile("[0-9]{4}[-\\s][0-9]{4}[-\\s][0-9]{4}[-\\s][0-9]{4}"),
            Pattern.compile("([a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,})"),
            Pattern.compile("(身份证|ID|护照|驾驶证).*[0-9]+"),
            Pattern.compile("(银行|银行卡|信用卡).*[0-9]+"),
            Pattern.compile("(密码|password|token|密钥|secret).*"),
            Pattern.compile("(手机号|电话).*[0-9]{7,}"),
            Pattern.compile("(地址|住址).*(省|市|区|街道)"),
            Pattern.compile("(QQ|微信|邮箱).*")
    );

    private final List<String> suspiciousKeywords = Arrays.asList(
            "sql注入", "xss", "跨站", "攻击", "漏洞", "利用", "exp", "payload",
            "admin", "root", "sudo", "rm -rf", "格式化", "删除", "销毁",
            "挖矿", "病毒", "木马", "恶意", "钓鱼", "诈骗"
    );

    public SecurityCheckResult checkInput(String input) {
        SecurityCheckResult result = new SecurityCheckResult();
        result.setInput(input);
        result.setTimestamp(LocalDateTime.now());
        result.setPassed(true);
        result.setViolations(new ArrayList<>());

        if (!securityConfig.isEnabled()) {
            log.debug("安全检查已禁用");
            return result;
        }

        if (securityConfig.isInjectionProtection()) {
            checkInjection(input, result);
        }

        if (securityConfig.isJailbreakProtection()) {
            checkJailbreak(input, result);
        }

        if (securityConfig.isDataPollutionProtection()) {
            checkDataPollution(input, result);
        }

        if (securityConfig.isSensitiveInfoProtection()) {
            checkSensitiveInfo(input, result);
        }

        if (!result.getViolations().isEmpty()) {
            result.setPassed(false);
            log.warn("安全检查失败，发现 {} 个违规: {}", 
                    result.getViolations().size(), result.getViolations());
        }

        return result;
    }

    private void checkInjection(String input, SecurityCheckResult result) {
        for (Pattern pattern : injectionPatterns) {
            if (pattern.matcher(input).find()) {
                result.getViolations().add("PROMPT_INJECTION");
                log.debug("检测到提示注入: {}", input);
                break;
            }
        }
    }

    private void checkJailbreak(String input, SecurityCheckResult result) {
        for (Pattern pattern : jailbreakPatterns) {
            if (pattern.matcher(input).find()) {
                result.getViolations().add("JAILBREAK_ATTEMPT");
                log.debug("检测到越狱尝试: {}", input);
                break;
            }
        }
    }

    private void checkDataPollution(String input, SecurityCheckResult result) {
        if (input.length() > 10000) {
            result.getViolations().add("DATA_POLLUTION");
            log.debug("检测到数据污染（超长输入）: 长度={}", input.length());
            return;
        }

        int repeatedPatterns = 0;
        String lowerInput = input.toLowerCase();
        for (String keyword : suspiciousKeywords) {
            if (lowerInput.contains(keyword)) {
                repeatedPatterns++;
            }
        }

        if (repeatedPatterns > 3) {
            result.getViolations().add("DATA_POLLUTION");
            log.debug("检测到数据污染（可疑关键词过多）: {}", repeatedPatterns);
        }
    }

    private void checkSensitiveInfo(String input, SecurityCheckResult result) {
        for (Pattern pattern : sensitiveInfoPatterns) {
            if (pattern.matcher(input).find()) {
                result.getViolations().add("SENSITIVE_INFO_LEAK");
                log.debug("检测到敏感信息泄露: {}", input);
                break;
            }
        }
    }

    public String sanitizeInput(String input) {
        if (!securityConfig.isSensitiveInfoProtection()) {
            return input;
        }

        String sanitized = input;

        sanitized = sanitized.replaceAll("[0-9]{11}", "***********");
        sanitized = sanitized.replaceAll("[0-9]{18}", "******************");
        sanitized = sanitized.replaceAll("[0-9]{4}[-\\s][0-9]{4}[-\\s][0-9]{4}[-\\s][0-9]{4}", "****-****-****-****");
        sanitized = sanitized.replaceAll("([a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,})", "***@***.***");
        sanitized = sanitized.replaceAll("(密码|password|token|密钥|secret)\\s*[:：]?\\s*[\\w\\d]+", "$1: ****");

        log.debug("输入已脱敏: {}", sanitized);
        return sanitized;
    }

    public Map<String, Object> getSecurityStatus() {
        return Map.of(
                "enabled", securityConfig.isEnabled(),
                "injectionProtection", securityConfig.isInjectionProtection(),
                "jailbreakProtection", securityConfig.isJailbreakProtection(),
                "dataPollutionProtection", securityConfig.isDataPollutionProtection(),
                "sensitiveInfoProtection", securityConfig.isSensitiveInfoProtection()
        );
    }

    @Data
    public static class SecurityCheckResult {
        private String input;
        private boolean passed;
        private List<String> violations;
        private LocalDateTime timestamp;
    }
}
