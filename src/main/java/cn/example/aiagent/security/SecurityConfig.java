package cn.example.aiagent.security;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Data
@Configuration
@ConfigurationProperties(prefix = "ai.agent.security")
public class SecurityConfig {

    private boolean enabled = true;

    private boolean injectionProtection = true;

    private boolean jailbreakProtection = true;

    private boolean dataPollutionProtection = true;

    private boolean sensitiveInfoProtection = true;
}
