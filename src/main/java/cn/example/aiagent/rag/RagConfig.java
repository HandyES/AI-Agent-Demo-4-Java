package cn.example.aiagent.rag;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Data
@Configuration
@ConfigurationProperties(prefix = "ai.agent.rag")
public class RagConfig {

    private String mode = "basic";

    private EmbeddingConfig embedding = new EmbeddingConfig();

    @Data
    public static class EmbeddingConfig {
        private String provider = "mock";
    }
}
