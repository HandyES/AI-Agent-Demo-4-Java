package cn.example.aiagent.observability;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Data
@Configuration
@ConfigurationProperties(prefix = "ai.agent.observability")
public class ObservabilityConfig {

    private boolean logging = true;

    private boolean tracing = true;

    private boolean metrics = true;
}
